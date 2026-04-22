import 'package:flutter/material.dart';
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
          title: const Text('移除成员'),
          content: Text('确认将 ${member.nickname} 移出当前家庭吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('确认移除'),
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
        title: const Text('家庭共养管理'),
        actions: [
          if (familyDetail != null &&
              _canManageFamily(familyDetail.currentUserRole))
            TextButton(
              onPressed: _openInvitationPage,
              child: const Text('邀请'),
            ),
        ],
      ),
      body: _buildBody(familyDetail),
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
          Text(
            _errorMessage!,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFFB91C1C)),
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: _loadFamilyDetail,
            child: const Text('重新加载'),
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
        PageSection(
          title: familyDetail.familyName,
          description: '家庭共养页的重点不是社交，而是清晰的角色、成员关系和共享宠物边界。',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _InfoRow(
                  label: '当前角色',
                  value: _toLocalizedRole(familyDetail.currentUserRole)),
              const SizedBox(height: 12),
              _InfoRow(label: '成员数量', value: '${familyDetail.memberCount} 人'),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '成员列表',
          description: '当前支持查看成员、调整角色和移除成员；权限收紧策略以服务端判断为准。',
          child: Column(
            children: familyDetail.members
                .map(
                  (FamilyMemberSnapshot member) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _FamilyMemberCard(
                      member: member,
                      currentUserId: widget.currentUser.userId,
                      currentUserRole: familyDetail.currentUserRole,
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
          description: '当前家庭下的宠物默认都在这里可见，邀请时会再明确勾选共享范围。',
          child: Column(
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
          description: '邀请结果当前先在家庭页回看，后续再补被邀请方接受和拒绝链路。',
          child: familyDetail.pendingInvitations.isEmpty
              ? const Text('当前没有待处理邀请')
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
          title: '权限说明',
          description: 'owner 拥有全部权限；admin 可管理成员与提醒；member 仅处理被授权的查看和协作动作。',
          child: const Text('敏感操作仍以服务端角色校验为准。'),
        ),
      ],
    );
  }
}

class _FamilyMemberCard extends StatelessWidget {
  const _FamilyMemberCard({
    required this.member,
    required this.currentUserId,
    required this.currentUserRole,
    this.onPromoteToAdmin,
    this.onDemoteToMember,
    this.onRemove,
  });

  final FamilyMemberSnapshot member;
  final String currentUserId;
  final String currentUserRole;
  final VoidCallback? onPromoteToAdmin;
  final VoidCallback? onDemoteToMember;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    final bool canOperate = onPromoteToAdmin != null ||
        onDemoteToMember != null ||
        onRemove != null;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  member.nickname,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 6),
                Text(
                  '${member.mobile} · ${_toLocalizedRole(member.role)}',
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
                ),
              ],
            ),
          ),
          if (member.userId == currentUserId)
            _RoleTag(label: '我自己')
          else if (canOperate)
            PopupMenuButton<String>(
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
                    child: Text('移除成员'),
                  ));
                }
                return items;
              },
            )
          else
            _RoleTag(label: _toLocalizedRole(member.role)),
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
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          const Icon(Icons.pets_outlined, color: Color(0xFF166534)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(pet.petName,
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 6),
                Text(
                  '${_toLocalizedPetType(pet.petType)} · ${pet.breed}',
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
                ),
              ],
            ),
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

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            invitation.inviteeMobile,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 6),
          Text(
            '${_toLocalizedRole(invitation.role)} · 邀请码 ${invitation.inviteCode}',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
          const SizedBox(height: 6),
          Text(
            petNames.isEmpty ? '未配置共享宠物' : '共享宠物：${petNames.join(' / ')}',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
        ],
      ),
    );
  }
}

class _RoleTag extends StatelessWidget {
  const _RoleTag({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFE2E8F0),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(label),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 88,
          child: Text(
            label,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
        ),
        Expanded(
          child: Text(value, style: Theme.of(context).textTheme.titleMedium),
        ),
      ],
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

String _toLocalizedPetType(String petType) {
  switch (petType) {
    case 'cat':
      return '猫';
    case 'dog':
      return '犬';
    default:
      return petType;
  }
}
