/// 短信验证码发送结果。
class AuthSmsSendSnapshot {
  const AuthSmsSendSnapshot({
    required this.mobile,
    required this.scene,
    required this.mockedCode,
    required this.expiresInSeconds,
    required this.resendInSeconds,
  });

  final String mobile;
  final String scene;
  final String mockedCode;
  final int expiresInSeconds;
  final int resendInSeconds;
}
