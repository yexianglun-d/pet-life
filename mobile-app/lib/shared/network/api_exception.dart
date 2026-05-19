/// 接口调用异常。
class ApiException implements Exception {
  const ApiException(
    this.message, {
    this.kind = ApiExceptionKind.business,
    this.responseCode,
  });

  final String message;
  final ApiExceptionKind kind;
  final String? responseCode;

  @override
  String toString() => message;
}

enum ApiExceptionKind {
  business,
  network,
  timeout,
  sessionExpired,
  server,
  data,
}
