import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';

/// 当前用户快照。
class CurrentUserSnapshot {
  const CurrentUserSnapshot({
    required this.userId,
    required this.nickname,
    required this.familyName,
    this.currentPetId,
    this.currentPet,
  });

  final String userId;
  final String nickname;
  final String familyName;
  final String? currentPetId;
  final PetProfileSnapshot? currentPet;
}
