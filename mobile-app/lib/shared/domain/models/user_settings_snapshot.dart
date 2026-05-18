/// 用户设置快照。
class UserSettingsSnapshot {
  const UserSettingsSnapshot({
    required this.userId,
    required this.mobile,
    required this.nickname,
    required this.notificationEnabled,
    required this.privacyLevel,
    this.cityCode,
    this.cityName,
    this.currentPetId,
  });

  final String userId;
  final String mobile;
  final String nickname;
  final String? cityCode;
  final String? cityName;
  final String? currentPetId;
  final bool notificationEnabled;
  final String privacyLevel;
}
