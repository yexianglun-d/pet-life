/// 家庭详情快照。
class FamilyDetailSnapshot {
  const FamilyDetailSnapshot({
    required this.familyId,
    required this.familyName,
    required this.memberCount,
    required this.currentUserRole,
    required this.members,
    required this.sharedPets,
    required this.pendingInvitations,
  });

  final String familyId;
  final String familyName;
  final int memberCount;
  final String currentUserRole;
  final List<FamilyMemberSnapshot> members;
  final List<FamilySharedPetSnapshot> sharedPets;
  final List<FamilyInvitationSnapshot> pendingInvitations;
}

/// 家庭成员快照。
class FamilyMemberSnapshot {
  const FamilyMemberSnapshot({
    required this.memberId,
    required this.userId,
    required this.nickname,
    required this.mobile,
    required this.role,
    required this.inviteStatus,
    this.joinedAt,
  });

  final String memberId;
  final String userId;
  final String nickname;
  final String mobile;
  final String role;
  final String inviteStatus;
  final DateTime? joinedAt;
}

/// 家庭共享宠物快照。
class FamilySharedPetSnapshot {
  const FamilySharedPetSnapshot({
    required this.petId,
    required this.petName,
    required this.petType,
    required this.breed,
  });

  final String petId;
  final String petName;
  final String petType;
  final String breed;
}

/// 家庭邀请快照。
class FamilyInvitationSnapshot {
  const FamilyInvitationSnapshot({
    required this.invitationId,
    required this.inviteeMobile,
    required this.role,
    required this.sharedPetIds,
    required this.inviteCode,
    required this.status,
    this.expiredAt,
    this.createdAt,
  });

  final String invitationId;
  final String inviteeMobile;
  final String role;
  final List<String> sharedPetIds;
  final String inviteCode;
  final String status;
  final DateTime? expiredAt;
  final DateTime? createdAt;
}
