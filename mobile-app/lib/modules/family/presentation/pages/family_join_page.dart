import 'package:flutter/material.dart';
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
        const SnackBar(content: Text('已拒绝当前邀请')),
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
      appBar: AppBar(title: const Text('通过邀请码加入家庭')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          PageSection(
            title: '邀请码',
            description: '输入家庭邀请码后，先确认角色、共享宠物和邀请状态，再决定是否加入。',
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
                      hintText: '请输入邀请人提供的邀请码',
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
                    Text(
                      _errorMessage!,
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium
                          ?.copyWith(color: const Color(0xFFB91C1C)),
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
              description: '接受后会切换到本次共享宠物所在家庭上下文，方便直接查看对应档案与待办。',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _InfoRow(
                      label: '邀请人', value: invitationPreview.inviterNickname),
                  const SizedBox(height: 12),
                  _InfoRow(
                    label: '加入身份',
                    value: _toLocalizedRole(invitationPreview.role),
                  ),
                  const SizedBox(height: 12),
                  _InfoRow(
                    label: '邀请状态',
                    value:
                        _toLocalizedInvitationStatus(invitationPreview.status),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    '共享宠物',
                    style: Theme.of(context).textTheme.titleSmall,
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
      bottomNavigationBar:
          invitationPreview == null || invitationPreview.status != 'pending'
              ? null
              : SafeArea(
                  minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                  child: Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: _isSubmitting ? null : _rejectInvitation,
                          child: const Text('拒绝邀请'),
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
          width: 84,
          child: Text(
            label,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
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
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: const Color(0xFFDCFCE7),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(Icons.pets_outlined, color: Color(0xFF166534)),
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
      return '待处理';
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
