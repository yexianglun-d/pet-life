/// 接口调用异常。
class ApiException implements Exception {
  const ApiException(
    this.message, {
    this.kind = ApiExceptionKind.business,
  });

  final String message;
  final ApiExceptionKind kind;

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
