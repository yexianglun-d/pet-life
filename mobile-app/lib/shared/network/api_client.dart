import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:petlife_mobile_app/shared/network/api_exception.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 统一接口客户端。
///
/// 当前客户端除了承担响应信封解析，还负责在登录态失效时自动尝试刷新令牌。
/// 这样页面层不需要散落处理 401、刷新重试和会话清理逻辑。
class ApiClient {
  ApiClient({
    required Uri baseUri,
    AppSessionStore? sessionStore,
    http.Client? httpClient,
  })  : _baseUri = baseUri,
        _sessionStore = sessionStore,
        _httpClient = httpClient ?? http.Client();

  final Uri _baseUri;
  final AppSessionStore? _sessionStore;
  final http.Client _httpClient;

  static const Duration _requestTimeout = Duration(seconds: 20);

  Future<bool>? _refreshSessionFuture;

  Future<dynamic> getData(String path) {
    return _sendRequest(
      path: path,
      requestFactory: (Map<String, String> headers) =>
          _httpClient.get(_baseUri.resolve(path), headers: headers),
    );
  }

  Future<dynamic> postData(
    String path, {
    required Map<String, Object?> body,
  }) {
    return _sendRequest(
      path: path,
      requestFactory: (Map<String, String> headers) => _httpClient.post(
        _baseUri.resolve(path),
        headers: headers,
        body: jsonEncode(body),
      ),
    );
  }

  Future<dynamic> postMultipartData(
    String path, {
    required Map<String, String> fields,
    required String filePath,
    String fileField = 'file',
  }) {
    return _sendRequest(
      path: path,
      requestFactory: (Map<String, String> headers) async {
        final http.MultipartRequest request = http.MultipartRequest(
          'POST',
          _baseUri.resolve(path),
        );
        final Map<String, String> multipartHeaders =
            Map<String, String>.from(headers)..remove('Content-Type');
        request.headers.addAll(multipartHeaders);
        request.fields.addAll(fields);
        request.files.add(await http.MultipartFile.fromPath(
          fileField,
          filePath,
        ));
        return http.Response.fromStream(await _httpClient.send(request));
      },
    );
  }

  Future<dynamic> patchData(
    String path, {
    required Map<String, Object?> body,
  }) {
    return _sendRequest(
      path: path,
      requestFactory: (Map<String, String> headers) => _httpClient.patch(
        _baseUri.resolve(path),
        headers: headers,
        body: jsonEncode(body),
      ),
    );
  }

  Future<dynamic> deleteData(String path) {
    return _sendRequest(
      path: path,
      requestFactory: (Map<String, String> headers) =>
          _httpClient.delete(_baseUri.resolve(path), headers: headers),
    );
  }

  Future<dynamic> _sendRequest({
    required String path,
    required Future<http.Response> Function(Map<String, String> headers)
        requestFactory,
  }) async {
    http.Response response = await _performHttpRequest(
      requestFactory,
      await _buildHeaders(),
    );
    if (_shouldRecoverSession(path: path, response: response)) {
      final bool recovered = await _tryRefreshSession();
      if (!recovered) {
        throw const ApiException(
          '登录状态已失效，请重新登录',
          kind: ApiExceptionKind.sessionExpired,
        );
      }
      response = await _performHttpRequest(
        requestFactory,
        await _buildHeaders(),
      );
      if (response.statusCode == 401) {
        await _sessionStore?.clear();
        throw const ApiException(
          '登录状态已失效，请重新登录',
          kind: ApiExceptionKind.sessionExpired,
        );
      }
    }
    return _extractData(path: path, response: response);
  }

  Future<http.Response> _performHttpRequest(
    Future<http.Response> Function(Map<String, String> headers) requestFactory,
    Map<String, String> headers,
  ) async {
    try {
      return await requestFactory(headers).timeout(_requestTimeout);
    } on TimeoutException {
      throw const ApiException(
        '网络请求超时，请稍后重试',
        kind: ApiExceptionKind.timeout,
      );
    } on http.ClientException {
      throw const ApiException(
        '网络连接不可用，请检查网络后重试',
        kind: ApiExceptionKind.network,
      );
    }
  }

  bool _shouldRecoverSession({
    required String path,
    required http.Response response,
  }) {
    return !_isAuthPath(path) &&
        response.statusCode == 401 &&
        _sessionStore != null;
  }

  bool _isAuthPath(String path) {
    return path.startsWith('/api/v1/auth/');
  }

  Future<bool> _tryRefreshSession() async {
    if (_refreshSessionFuture != null) {
      return _refreshSessionFuture!;
    }

    // 所有并发 401 共用同一次刷新请求，避免同一时刻重复轮换 session。
    final Future<bool> refreshFuture = _performSessionRefresh();
    _refreshSessionFuture = refreshFuture;
    try {
      return await refreshFuture;
    } finally {
      _refreshSessionFuture = null;
    }
  }

  Future<bool> _performSessionRefresh() async {
    final AppSessionStore? sessionStore = _sessionStore;
    if (sessionStore == null) {
      return false;
    }

    final String? refreshToken = await sessionStore.readRefreshToken();
    if (refreshToken == null) {
      await sessionStore.clear();
      return false;
    }

    final http.Response response = await _performHttpRequest(
      (Map<String, String> headers) => _httpClient.post(
        _baseUri.resolve('/api/v1/auth/refresh'),
        headers: headers,
        body: jsonEncode(<String, Object?>{
          'refresh_token': refreshToken,
        }),
      ),
      await _buildHeaders(includeAuthorization: false),
    );

    if (response.statusCode == 401 || response.statusCode == 403) {
      await sessionStore.clear();
      return false;
    }

    final Map<String, dynamic>? payload = _decodeEnvelope(response);
    if (payload == null) {
      throw const ApiException(
        '登录状态暂时无法确认，请稍后重试',
        kind: ApiExceptionKind.server,
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        _readEnvelopeMessage(payload, fallback: '登录状态暂时无法刷新，请稍后重试'),
        kind: ApiExceptionKind.server,
        responseCode: _readEnvelopeCode(payload),
      );
    }

    final String code = payload['code']?.toString() ?? '';
    if (code != 'OK') {
      await sessionStore.clear();
      return false;
    }

    final Object? data = payload['data'];
    if (data is! Map<String, dynamic>) {
      throw const ApiException(
        '登录状态返回结构不合法',
        kind: ApiExceptionKind.data,
      );
    }

    final String accessToken = data['access_token']?.toString().trim() ?? '';
    final String nextRefreshToken =
        data['refresh_token']?.toString().trim() ?? '';
    if (accessToken.isEmpty || nextRefreshToken.isEmpty) {
      throw const ApiException(
        '登录状态返回结构不完整',
        kind: ApiExceptionKind.data,
      );
    }

    await sessionStore.saveSession(
      accessToken: accessToken,
      refreshToken: nextRefreshToken,
    );
    return true;
  }

  Future<Map<String, String>> _buildHeaders({
    bool includeAuthorization = true,
  }) async {
    final Map<String, String> headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    };

    if (!includeAuthorization) {
      return headers;
    }

    final String? accessToken = await _sessionStore?.readAccessToken();
    if (accessToken != null) {
      headers['Authorization'] = 'Bearer $accessToken';
    }

    return headers;
  }

  dynamic _extractData({
    required String path,
    required http.Response response,
  }) {
    final Map<String, dynamic>? payload = _decodeEnvelope(response);
    if (payload == null) {
      throw const ApiException(
        '服务返回异常，请稍后重试',
        kind: ApiExceptionKind.data,
      );
    }

    if (response.statusCode == 401 && !_isAuthPath(path)) {
      throw const ApiException(
        '登录状态已失效，请重新登录',
        kind: ApiExceptionKind.sessionExpired,
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        _readEnvelopeMessage(payload, fallback: '请求失败'),
        kind: response.statusCode >= 500
            ? ApiExceptionKind.server
            : ApiExceptionKind.business,
        responseCode: _readEnvelopeCode(payload),
      );
    }

    final String code = payload['code']?.toString() ?? '';
    if (code != 'OK') {
      throw ApiException(
        _readEnvelopeMessage(payload, fallback: '业务处理失败'),
        responseCode: _readEnvelopeCode(payload),
      );
    }

    return payload['data'];
  }

  Map<String, dynamic>? _decodeEnvelope(http.Response response) {
    try {
      final dynamic payload = jsonDecode(response.body);
      if (payload is Map<String, dynamic>) {
        return payload;
      }
      if (payload is Map) {
        return payload.map(
          (Object? key, Object? value) => MapEntry(key.toString(), value),
        );
      }
    } on FormatException {
      return null;
    }
    return null;
  }

  String _readEnvelopeMessage(
    Map<String, dynamic> payload, {
    required String fallback,
  }) {
    final Object? message = payload['message'];
    return message == null || message.toString().trim().isEmpty
        ? fallback
        : message.toString();
  }

  String? _readEnvelopeCode(Map<String, dynamic> payload) {
    final Object? code = payload['code'];
    final String value = code?.toString().trim() ?? '';
    return value.isEmpty ? null : value;
  }
}
