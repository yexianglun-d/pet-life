import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_preview_snapshot.dart';

/// 通过邀请码加入家庭页面。
class FamilyJoinPage extends StatefulWidget {
  const FamilyJoinPage({super.key});

  @override
  State<FamilyJoinPage> createState() => _FamilyJoinPageState();
}

class _FamilyJoinPageState extends State<FamilyJoinPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _inviteCodeController;
  FamilyInvitationPreviewSnapshot? _invitationPreview;
  String? _errorMessage;
  bool _isLoadingPreview = false;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _inviteCodeController = TextEditingController();
  }

  @override
  void dispose() {
    _inviteCodeController.dispose();
    super.dispose();
  }

  Future<void> _loadInvitationPreview() async {
    if (_isLoadingPreview || !_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoadingPreview = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final FamilyInvitationPreviewSnapshot invitationPreview = await repository
          .getFamilyInvitationPreview(_inviteCodeController.text.trim());
      if (!mounted) {
        return;
      }
      setState(() {
        _invitationPreview = invitationPreview;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _invitationPreview = null;
        _errorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingPreview = false;
        });
      }
    }
  }

  Future<void> _acceptInvitation() async {
    final FamilyInvitationPreviewSnapshot? invitationPreview =
        _invitationPreview;
    if (_isSubmitting || invitationPreview == null) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final FamilyDetailSnapshot familyDetail =
          await repository.acceptFamilyInvitation(invitationPreview.inviteCode);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已加入 ${familyDetail.familyName}')),
      );
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  Future<void> _rejectInvitation() async {
    final FamilyInvitationPreviewSnapshot? invitationPreview =
        _invitationPreview;
    if (_isSubmitting || invitationPreview == null) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final FamilyInvitationPreviewSnapshot rejectedPreview =
          await repository.rejectFamilyInvitation(invitationPreview.inviteCode);
      if (!mounted) {
        return;
      }
      setState(() {
        _invitationPreview = rejectedPreview;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('这次邀请已经拒绝')),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final FamilyInvitationPreviewSnapshot? invitationPreview =
        _invitationPreview;

    return Scaffold(
      appBar: AppBar(title: const Text('加入家庭')),
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
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const _JoinHeroCard(),
            const SizedBox(height: 16),
            PageSection(
              title: '输入邀请码',
              description: '先看看这是哪个家庭、谁邀请了你、会共享哪些宠物，再决定要不要加入。',
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextFormField(
                      controller: _inviteCodeController,
                      textCapitalization: TextCapitalization.characters,
                      decoration: const InputDecoration(
                        labelText: '邀请码',
                        hintText: '请输入邀请人发来的邀请码',
                      ),
                      validator: (String? value) {
                        final String inviteCode = value?.trim() ?? '';
                        if (inviteCode.isEmpty) {
                          return '请输入邀请码';
                        }
                        if (inviteCode.length < 8) {
                          return '邀请码格式不正确';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: _isLoadingPreview || _isSubmitting
                            ? null
                            : _loadInvitationPreview,
                        child: Text(_isLoadingPreview ? '查询中...' : '查看邀请'),
                      ),
                    ),
                    if (_errorMessage != null) ...[
                      const SizedBox(height: 12),
                      CompanionEmptyState(
                        title: '没有找到这条邀请',
                        description: _errorMessage!,
                        icon: Icons.search_off_rounded,
                      ),
                    ],
                  ],
                ),
              ),
            ),
            if (invitationPreview != null) ...[
              const SizedBox(height: 16),
              PageSection(
                title: invitationPreview.familyName,
                description: '确认角色和共享范围后再加入，进入后当前家庭上下文会自动切换到这次共养关系。',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        CompanionPill(
                          label: '邀请人 ${invitationPreview.inviterNickname}',
                          backgroundColor: AppThemePalette.surfaceRaised,
                        ),
                        CompanionPill(
                          label: _toLocalizedRole(invitationPreview.role),
                          backgroundColor: const Color(0xFFFFE8D9),
                          foregroundColor: AppThemePalette.primaryDeep,
                        ),
                        CompanionPill(
                          label: _toLocalizedInvitationStatus(
                            invitationPreview.status,
                          ),
                          backgroundColor: _statusBackgroundColor(
                            invitationPreview.status,
                          ),
                          foregroundColor: _statusForegroundColor(
                            invitationPreview.status,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    _InfoRow(
                      label: '受邀手机号',
                      value: invitationPreview.inviteeMobile,
                    ),
                    const SizedBox(height: 12),
                    _InfoRow(
                      label: '邀请码',
                      value: invitationPreview.inviteCode,
                    ),
                    const SizedBox(height: 18),
                    Text(
                      '这次会一起照顾的宠物',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 12),
                    ...invitationPreview.sharedPets.map(
                      (FamilySharedPetSnapshot pet) => Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: _SharedPetTile(pet: pet),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar:
          invitationPreview == null || invitationPreview.status != 'pending'
              ? null
              : SafeArea(
                  minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                  child: CompanionCard(
                    padding: const EdgeInsets.all(12),
                    color: AppThemePalette.surface,
                    radius: 26,
                    child: Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            onPressed: _isSubmitting ? null : _rejectInvitation,
                            child: const Text('先拒绝'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: FilledButton(
                            onPressed: _isSubmitting ? null : _acceptInvitation,
                            child: Text(_isSubmitting ? '处理中...' : '接受加入'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
    );
  }
}

class _JoinHeroCard extends StatelessWidget {
  const _JoinHeroCard();

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDC),
          Color(0xFFFFFAF4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '邀请码加入',
            icon: Icons.key_rounded,
            backgroundColor: Color(0xFFFFE2D1),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            '确认后再走进这个家庭',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '每个邀请都会写清楚谁邀请你、共享哪些宠物，以及你在家庭里的身份。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 92,
          child: Text(
            label,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ),
      ],
    );
  }
}

class _SharedPetTile extends StatelessWidget {
  const _SharedPetTile({required this.pet});

  final FamilySharedPetSnapshot pet;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(14),
      color: AppThemePalette.surfaceRaised,
      radius: 22,
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
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
                    style: Theme.of(context).textTheme.titleSmall),
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
        ],
      ),
    );
  }
}

String _toLocalizedRole(String role) {
  switch (role) {
    case 'admin':
      return '管理员';
    case 'member':
      return '普通成员';
    default:
      return role;
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

Color _statusBackgroundColor(String status) {
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

Color _statusForegroundColor(String status) {
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
