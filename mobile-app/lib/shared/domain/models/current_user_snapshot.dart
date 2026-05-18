import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';

/// 当前用户快照。
class CurrentUserSnapshot {
  const CurrentUserSnapshot({
    required this.userId,
    required this.mobile,
    required this.nickname,
    required this.familyName,
    this.cityCode,
    this.cityName,
    this.currentPetId,
    this.currentPet,
  });

  final String userId;
  final String mobile;
  final String nickname;
  final String familyName;
  final String? cityCode;
  final String? cityName;
  final String? currentPetId;
  final PetProfileSnapshot? currentPet;
}
