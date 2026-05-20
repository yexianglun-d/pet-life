/// App Push 设备 Token 注册结果。
///
/// 当前移动端不接真实 Push SDK，本模型只承接服务端真实接口返回。
/// 完整 token 不会从服务端回显；页面只应在拿到真实 SDK token 后调用注册接口。
class PushDeviceTokenSnapshot {
  const PushDeviceTokenSnapshot({
    required this.deviceTokenId,
    required this.userId,
    required this.platform,
    required this.providerCode,
    required this.deviceTokenSuffix,
    required this.enabled,
    this.deviceId,
    this.appVersion,
    this.lastRegisteredAt,
  });

  final String deviceTokenId;
  final String userId;
  final String platform;
  final String providerCode;
  final String deviceTokenSuffix;
  final String? deviceId;
  final String? appVersion;
  final bool enabled;
  final DateTime? lastRegisteredAt;
}
