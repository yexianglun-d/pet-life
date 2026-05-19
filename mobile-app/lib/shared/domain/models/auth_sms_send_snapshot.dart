/// 短信验证码发送结果。
class AuthSmsSendSnapshot {
  const AuthSmsSendSnapshot({
    required this.mobile,
    required this.scene,
    required this.sent,
    required this.expiresInSeconds,
    required this.resendInSeconds,
    required this.providerCode,
  });

  final String mobile;
  final String scene;
  final bool sent;
  final int expiresInSeconds;
  final int resendInSeconds;
  final String providerCode;
}
