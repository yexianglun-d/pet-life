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
    http.Response response = await requestFactory(await _buildHeaders());
    if (_shouldRecoverSession(path: path, response: response)) {
      final bool recovered = await _tryRefreshSession();
      if (!recovered) {
        throw const ApiException('登录状态已失效，请重新登录');
      }
      response = await requestFactory(await _buildHeaders());
      if (response.statusCode == 401) {
        await _sessionStore?.clear();
        throw const ApiException('登录状态已失效，请重新登录');
      }
    }
    return _extractData(response);
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

    try {
      final http.Response response = await _httpClient.post(
        _baseUri.resolve('/api/v1/auth/refresh'),
        headers: await _buildHeaders(includeAuthorization: false),
        body: jsonEncode(<String, Object?>{
          'refresh_token': refreshToken,
        }),
      );
      final Map<String, dynamic>? payload = _decodeEnvelope(response);
      if (payload == null ||
          response.statusCode < 200 ||
          response.statusCode >= 300) {
        await sessionStore.clear();
        return false;
      }

      final String code = payload['code']?.toString() ?? '';
      if (code != 'OK') {
        await sessionStore.clear();
        return false;
      }

      final Object? data = payload['data'];
      if (data is! Map<String, dynamic>) {
        await sessionStore.clear();
        return false;
      }

      final String accessToken = data['access_token']?.toString().trim() ?? '';
      final String nextRefreshToken =
          data['refresh_token']?.toString().trim() ?? '';
      if (accessToken.isEmpty || nextRefreshToken.isEmpty) {
        await sessionStore.clear();
        return false;
      }

      await sessionStore.saveSession(
        accessToken: accessToken,
        refreshToken: nextRefreshToken,
      );
      return true;
    } catch (_) {
      await sessionStore.clear();
      return false;
    }
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

  dynamic _extractData(http.Response response) {
    final Map<String, dynamic>? payload = _decodeEnvelope(response);
    if (payload == null) {
      throw const ApiException('接口返回结构不合法');
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(_readEnvelopeMessage(payload, fallback: '请求失败'));
    }

    final String code = payload['code']?.toString() ?? '';
    if (code != 'OK') {
      throw ApiException(_readEnvelopeMessage(payload, fallback: '业务处理失败'));
    }

    return payload['data'];
  }

  Map<String, dynamic>? _decodeEnvelope(http.Response response) {
    final dynamic payload = jsonDecode(response.body);
    if (payload is Map<String, dynamic>) {
      return payload;
    }
    if (payload is Map) {
      return payload.map(
        (Object? key, Object? value) => MapEntry(key.toString(), value),
      );
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
}
