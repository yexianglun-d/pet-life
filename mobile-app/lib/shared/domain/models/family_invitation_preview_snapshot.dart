import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';

/// 家庭邀请预览快照。
class FamilyInvitationPreviewSnapshot {
  const FamilyInvitationPreviewSnapshot({
    required this.invitationId,
    required this.familyId,
    required this.familyName,
    required this.inviterNickname,
    required this.inviteeMobile,
    required this.role,
    required this.sharedPets,
    required this.inviteCode,
    required this.status,
    this.expiredAt,
    this.createdAt,
  });

  final String invitationId;
  final String familyId;
  final String familyName;
  final String inviterNickname;
  final String inviteeMobile;
  final String role;
  final List<FamilySharedPetSnapshot> sharedPets;
  final String inviteCode;
  final String status;
  final DateTime? expiredAt;
  final DateTime? createdAt;
}
