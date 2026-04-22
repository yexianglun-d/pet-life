import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:petlife_mobile_app/shared/network/api_exception.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 统一接口客户端。
///
/// 当前阶段所有用户端请求都走统一响应信封 `code/message/data`，该客户端负责在一处完成
/// HTTP 状态校验、业务码校验和 JSON 解析，避免页面层反复编写样板逻辑。
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

  Future<dynamic> getData(String path) async {
    final http.Response response = await _httpClient.get(
      _baseUri.resolve(path),
      headers: await _buildHeaders(),
    );

    return _extractData(response);
  }

  Future<dynamic> postData(String path,
      {required Map<String, Object?> body}) async {
    final http.Response response = await _httpClient.post(
      _baseUri.resolve(path),
      headers: await _buildHeaders(),
      body: jsonEncode(body),
    );

    return _extractData(response);
  }

  Future<dynamic> patchData(String path,
      {required Map<String, Object?> body}) async {
    final http.Response response = await _httpClient.patch(
      _baseUri.resolve(path),
      headers: await _buildHeaders(),
      body: jsonEncode(body),
    );

    return _extractData(response);
  }

  Future<dynamic> deleteData(String path) async {
    final http.Response response = await _httpClient.delete(
      _baseUri.resolve(path),
      headers: await _buildHeaders(),
    );

    return _extractData(response);
  }

  Future<Map<String, String>> _buildHeaders() async {
    final Map<String, String> headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    };

    final String? accessToken = await _sessionStore?.readAccessToken();
    if (accessToken != null) {
      headers['Authorization'] = 'Bearer $accessToken';
    }

    return headers;
  }

  dynamic _extractData(http.Response response) {
    final dynamic payload = jsonDecode(response.body);
    if (payload is! Map<String, dynamic>) {
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

  String _readEnvelopeMessage(Map<String, dynamic> payload,
      {required String fallback}) {
    final Object? message = payload['message'];
    return message == null || message.toString().trim().isEmpty
        ? fallback
        : message.toString();
  }
}
