import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/family/presentation/pages/family_invitation_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';

/// 家庭共养管理页。
class FamilyManagementPage extends StatefulWidget {
  const FamilyManagementPage({
    super.key,
    required this.currentUser,
  });

  final CurrentUserSnapshot currentUser;

  @override
  State<FamilyManagementPage> createState() => _FamilyManagementPageState();
}

class _FamilyManagementPageState extends State<FamilyManagementPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  String? _errorMessage;
  FamilyDetailSnapshot? _familyDetail;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadFamilyDetail();
  }

  Future<void> _loadFamilyDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final FamilyDetailSnapshot familyDetail =
          await repository.getFamilyDetail();
      if (!mounted) {
        return;
      }
      setState(() {
        _familyDetail = familyDetail;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _openInvitationPage() async {
    final FamilyDetailSnapshot? familyDetail = _familyDetail;
    if (familyDetail == null) {
      return;
    }

    final bool? created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) =>
            FamilyInvitationPage(sharedPets: familyDetail.sharedPets),
      ),
    );
    if (!mounted || created != true) {
      return;
    }

    await _loadFamilyDetail();
  }

  Future<void> _updateMemberRole(
    FamilyMemberSnapshot member,
    String targetRole,
  ) async {
    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.updateFamilyMemberRole(
        memberId: member.memberId,
        role: targetRole,
      );
      if (!mounted) {
        return;
      }
      await _loadFamilyDetail();
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    }
  }

  Future<void> _removeMember(FamilyMemberSnapshot member) async {
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('移出共养家庭'),
          content: Text('确认将 ${member.nickname} 移出当前家庭吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('确认移出'),
            ),
          ],
        );
      },
    );
    if (confirmed != true || !mounted) {
      return;
    }

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.removeFamilyMember(member.memberId);
      if (!mounted) {
        return;
      }
      await _loadFamilyDetail();
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final FamilyDetailSnapshot? familyDetail = _familyDetail;

    return Scaffold(
      appBar: AppBar(
        title: const Text('家庭共养'),
        actions: [
          if (familyDetail != null &&
              _canManageFamily(familyDetail.currentUserRole))
            TextButton(
              onPressed: _openInvitationPage,
              child: const Text('邀请成员'),
            ),
        ],
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFFBF7),
              AppThemePalette.background,
            ],
          ),
        ),
        child: _buildBody(familyDetail),
      ),
    );
  }

  Widget _buildBody(FamilyDetailSnapshot? familyDetail) {
    if (_isLoading && familyDetail == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && familyDetail == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionEmptyState(
            title: '家庭信息暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadFamilyDetail,
          ),
        ],
      );
    }

    if (familyDetail == null) {
      return const SizedBox.shrink();
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _FamilyHeroCard(
          familyDetail: familyDetail,
          onInvitePressed: _canManageFamily(familyDetail.currentUserRole)
              ? _openInvitationPage
              : null,
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '一起照顾的人',
          description: '谁在陪着这只毛孩子、每个人能做什么，都整理在这里。',
          child: Column(
            children: familyDetail.members
                .map(
                  (FamilyMemberSnapshot member) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _FamilyMemberCard(
                      member: member,
                      currentUserId: widget.currentUser.userId,
                      onPromoteToAdmin: _canPromoteToAdmin(
                        familyDetail.currentUserRole,
                        widget.currentUser.userId,
                        member,
                      )
                          ? () => _updateMemberRole(member, 'admin')
                          : null,
                      onDemoteToMember: _canDemoteToMember(
                        familyDetail.currentUserRole,
                        widget.currentUser.userId,
                        member,
                      )
                          ? () => _updateMemberRole(member, 'member')
                          : null,
                      onRemove: _canRemoveMember(
                        familyDetail.currentUserRole,
                        widget.currentUser.userId,
                        member,
                      )
                          ? () => _removeMember(member)
                          : null,
                    ),
                  ),
                )
                .toList(),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '共享宠物',
          description: '这些毛孩子会在当前家庭里一起被照顾，邀请时也会按这里确认共享范围。',
          child: familyDetail.sharedPets.isEmpty
              ? const CompanionEmptyState(
                  title: '还没有共享宠物',
                  description: '等家庭里建立了宠物主档，这里就会自动整理出来。',
                  icon: Icons.pets_outlined,
                )
              : Column(
                  children: familyDetail.sharedPets
                      .map(
                        (FamilySharedPetSnapshot pet) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _SharedPetCard(pet: pet),
                        ),
                      )
                      .toList(),
                ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '待处理邀请',
          description: '新成员还没正式加入前，先在这里看看邀请状态和共享范围。',
          child: familyDetail.pendingInvitations.isEmpty
              ? const CompanionEmptyState(
                  title: '目前没有待处理邀请',
                  description: '如果想把家人拉进来一起照顾，可以从右上角发出新的邀请。',
                  icon: Icons.mail_outline_rounded,
                )
              : Column(
                  children: familyDetail.pendingInvitations
                      .map(
                        (FamilyInvitationSnapshot invitation) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _InvitationCard(
                            invitation: invitation,
                            sharedPets: familyDetail.sharedPets,
                          ),
                        ),
                      )
                      .toList(),
                ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '角色说明',
          description: '权限会影响谁能邀请、调角色和移除成员，最终仍以服务端校验为准。',
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _PermissionTip(
                title: '拥有者',
                description: '可以管理全部成员，也能决定谁进入这个共养家庭。',
              ),
              SizedBox(height: 10),
              _PermissionTip(
                title: '管理员',
                description: '适合日常一起照护的人，能协助管理成员与提醒安排。',
              ),
              SizedBox(height: 10),
              _PermissionTip(
                title: '普通成员',
                description: '负责被授权范围内的查看和协作，不承担家庭配置管理。',
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _FamilyHeroCard extends StatelessWidget {
  const _FamilyHeroCard({
    required this.familyDetail,
    required this.onInvitePressed,
  });

  final FamilyDetailSnapshot familyDetail;
  final VoidCallback? onInvitePressed;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFBF5),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '家庭共养',
            icon: Icons.groups_rounded,
            backgroundColor: Color(0xFFFFE2D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            familyDetail.familyName,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '把一起照顾毛孩子的人、宠物和邀请关系都整理清楚，协作时就不会乱。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '${familyDetail.memberCount} 位成员',
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '我的身份 ${_toLocalizedRole(familyDetail.currentUserRole)}',
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '共享宠物 ${familyDetail.sharedPets.length} 只',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
          if (onInvitePressed != null) ...[
            const SizedBox(height: 18),
            FilledButton(
              onPressed: onInvitePressed,
              child: const Text('邀请家人一起照顾'),
            ),
          ],
        ],
      ),
    );
  }
}

class _FamilyMemberCard extends StatelessWidget {
  const _FamilyMemberCard({
    required this.member,
    required this.currentUserId,
    this.onPromoteToAdmin,
    this.onDemoteToMember,
    this.onRemove,
  });

  final FamilyMemberSnapshot member;
  final String currentUserId;
  final VoidCallback? onPromoteToAdmin;
  final VoidCallback? onDemoteToMember;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    final bool canOperate = onPromoteToAdmin != null ||
        onDemoteToMember != null ||
        onRemove != null;

    return CompanionCard(
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: AppThemePalette.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(
              Icons.person_outline_rounded,
              color: AppThemePalette.primaryDeep,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(member.nickname,
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  member.mobile,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.muted,
                      ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    CompanionPill(
                      label: _toLocalizedRole(member.role),
                      backgroundColor: _roleBackgroundColor(member.role),
                      foregroundColor: _roleForegroundColor(member.role),
                    ),
                    CompanionPill(
                      label: member.userId == currentUserId ? '我自己' : '家庭成员',
                      backgroundColor: AppThemePalette.surface,
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (canOperate)
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_horiz_rounded),
              onSelected: (String value) {
                switch (value) {
                  case 'promote':
                    onPromoteToAdmin?.call();
                    break;
                  case 'demote':
                    onDemoteToMember?.call();
                    break;
                  case 'remove':
                    onRemove?.call();
                    break;
                }
              },
              itemBuilder: (BuildContext context) {
                final List<PopupMenuEntry<String>> items =
                    <PopupMenuEntry<String>>[];
                if (onPromoteToAdmin != null) {
                  items.add(const PopupMenuItem<String>(
                    value: 'promote',
                    child: Text('设为管理员'),
                  ));
                }
                if (onDemoteToMember != null) {
                  items.add(const PopupMenuItem<String>(
                    value: 'demote',
                    child: Text('设为普通成员'),
                  ));
                }
                if (onRemove != null) {
                  items.add(const PopupMenuItem<String>(
                    value: 'remove',
                    child: Text('移出家庭'),
                  ));
                }
                return items;
              },
            ),
        ],
      ),
    );
  }
}

class _SharedPetCard extends StatelessWidget {
  const _SharedPetCard({required this.pet});

  final FamilySharedPetSnapshot pet;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: const Color(0xFFE8F3E7),
              borderRadius: BorderRadius.circular(16),
            ),
            child:
                const Icon(Icons.pets_rounded, color: AppThemePalette.success),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(pet.petName,
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  '${_toLocalizedPetType(pet.petType)} · ${pet.breed}',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.muted,
                      ),
                ),
              ],
            ),
          ),
          const CompanionPill(
            label: '已共享',
            backgroundColor: Color(0xFFE8F3E7),
            foregroundColor: AppThemePalette.success,
          ),
        ],
      ),
    );
  }
}

class _InvitationCard extends StatelessWidget {
  const _InvitationCard({
    required this.invitation,
    required this.sharedPets,
  });

  final FamilyInvitationSnapshot invitation;
  final List<FamilySharedPetSnapshot> sharedPets;

  @override
  Widget build(BuildContext context) {
    final List<String> petNames = sharedPets
        .where((FamilySharedPetSnapshot pet) =>
            invitation.sharedPetIds.contains(pet.petId))
        .map((FamilySharedPetSnapshot pet) => pet.petName)
        .toList();

    return CompanionCard(
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  invitation.inviteeMobile,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              CompanionPill(
                label: _toLocalizedInvitationStatus(invitation.status),
                backgroundColor: _invitationBackgroundColor(invitation.status),
                foregroundColor: _invitationForegroundColor(invitation.status),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              CompanionPill(
                label: _toLocalizedRole(invitation.role),
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '邀请码 ${invitation.inviteCode}',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            petNames.isEmpty
                ? '这次邀请还没有勾选共享宠物。'
                : '共享宠物：${petNames.join(' / ')}',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
        ],
      ),
    );
  }
}

class _PermissionTip extends StatelessWidget {
  const _PermissionTip({
    required this.title,
    required this.description,
  });

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(14),
      color: AppThemePalette.surfaceRaised,
      radius: 20,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
        ],
      ),
    );
  }
}

bool _canManageFamily(String currentUserRole) {
  return currentUserRole == 'owner' || currentUserRole == 'admin';
}

bool _canPromoteToAdmin(
  String currentUserRole,
  String currentUserId,
  FamilyMemberSnapshot member,
) {
  if (member.userId == currentUserId || member.role == 'owner') {
    return false;
  }
  if (currentUserRole == 'owner') {
    return member.role == 'member';
  }
  return currentUserRole == 'admin' && member.role == 'member';
}

bool _canDemoteToMember(
  String currentUserRole,
  String currentUserId,
  FamilyMemberSnapshot member,
) {
  if (member.userId == currentUserId || member.role == 'owner') {
    return false;
  }
  if (currentUserRole == 'owner') {
    return member.role == 'admin';
  }
  return false;
}

bool _canRemoveMember(
  String currentUserRole,
  String currentUserId,
  FamilyMemberSnapshot member,
) {
  if (member.userId == currentUserId || member.role == 'owner') {
    return false;
  }
  if (currentUserRole == 'owner') {
    return true;
  }
  return currentUserRole == 'admin' && member.role == 'member';
}

String _toLocalizedRole(String role) {
  switch (role) {
    case 'owner':
      return '拥有者';
    case 'admin':
      return '管理员';
    case 'member':
      return '普通成员';
    default:
      return role;
  }
}

Color _roleBackgroundColor(String role) {
  switch (role) {
    case 'owner':
      return const Color(0xFFFFE5D6);
    case 'admin':
      return const Color(0xFFE8F3E7);
    default:
      return AppThemePalette.surface;
  }
}

Color _roleForegroundColor(String role) {
  switch (role) {
    case 'owner':
      return AppThemePalette.primaryDeep;
    case 'admin':
      return AppThemePalette.success;
    default:
      return AppThemePalette.title;
  }
}

String _toLocalizedInvitationStatus(String status) {
  switch (status) {
    case 'pending':
      return '待回应';
    case 'accepted':
      return '已接受';
    case 'rejected':
      return '已拒绝';
    case 'expired':
      return '已过期';
    default:
      return status;
  }
}

Color _invitationBackgroundColor(String status) {
  switch (status) {
    case 'accepted':
      return const Color(0xFFE8F3E7);
    case 'rejected':
      return const Color(0xFFF6DFDA);
    case 'expired':
      return const Color(0xFFF2E8DE);
    default:
      return const Color(0xFFFFE8D9);
  }
}

Color _invitationForegroundColor(String status) {
  switch (status) {
    case 'accepted':
      return AppThemePalette.success;
    case 'rejected':
      return AppThemePalette.danger;
    case 'expired':
      return AppThemePalette.muted;
    default:
      return AppThemePalette.primaryDeep;
  }
}

String _toLocalizedPetType(String petType) {
  switch (petType) {
    case 'cat':
      return '猫咪';
    case 'dog':
      return '狗狗';
    default:
      return petType;
  }
}
