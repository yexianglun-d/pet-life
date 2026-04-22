/// 家庭邀请草稿。
class FamilyInvitationDraft {
  const FamilyInvitationDraft({
    required this.inviteeMobile,
    required this.role,
    required this.sharedPetIds,
  });

  final String inviteeMobile;
  final String role;
  final List<String> sharedPetIds;
}
