import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_draft.dart';

/// 家庭邀请页。
class FamilyInvitationPage extends StatefulWidget {
  const FamilyInvitationPage({
    super.key,
    required this.sharedPets,
  });

  final List<FamilySharedPetSnapshot> sharedPets;

  @override
  State<FamilyInvitationPage> createState() => _FamilyInvitationPageState();
}

class _FamilyInvitationPageState extends State<FamilyInvitationPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _mobileController;
  late String _role;
  late Set<String> _selectedPetIds;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _mobileController = TextEditingController();
    _role = 'member';
    _selectedPetIds = widget.sharedPets
        .map((FamilySharedPetSnapshot pet) => pet.petId)
        .toSet();
  }

  @override
  void dispose() {
    _mobileController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isSubmitting || !_formKey.currentState!.validate()) {
      return;
    }
    if (_selectedPetIds.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('至少选择一只共享宠物')),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final invitation = await repository.createFamilyInvitation(
        FamilyInvitationDraft(
          inviteeMobile: _mobileController.text.trim(),
          role: _role,
          sharedPetIds: _selectedPetIds.toList(),
        ),
      );
      if (!mounted) {
        return;
      }

      await showDialog<void>(
        context: context,
        builder: (BuildContext context) {
          return AlertDialog(
            title: const Text('邀请已创建'),
            content: Text('邀请码：${invitation.inviteCode}'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('知道了'),
              ),
            ],
          );
        },
      );
      if (!mounted) {
        return;
      }

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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('邀请家人')),
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
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _InvitationHeroCard(sharedPetCount: widget.sharedPets.length),
              const SizedBox(height: 16),
              PageSection(
                title: '邀请对象',
                description: '先把对方手机号填好，方便这次邀请准确送达。',
                child: TextFormField(
                  controller: _mobileController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: '手机号',
                    hintText: '请输入被邀请人的手机号',
                  ),
                  validator: (String? value) {
                    final String mobile = value?.trim() ?? '';
                    if (mobile.isEmpty) {
                      return '请输入手机号';
                    }
                    if (mobile.length < 11) {
                      return '手机号格式不正确';
                    }
                    return null;
                  },
                ),
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '邀请身份',
                description: '普通成员适合协助照护，管理员可以一起管理提醒和家庭成员。',
                child: DropdownButtonFormField<String>(
                  initialValue: _role,
                  decoration: const InputDecoration(labelText: '角色'),
                  items: const [
                    DropdownMenuItem(value: 'member', child: Text('普通成员')),
                    DropdownMenuItem(value: 'admin', child: Text('管理员')),
                  ],
                  onChanged: (String? value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
                      _role = value;
                    });
                  },
                ),
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '共享宠物范围',
                description: '邀请时明确共享哪些宠物，能避免成员加入后看到超出预期的档案。',
                child: Column(
                  children: widget.sharedPets
                      .map(
                        (FamilySharedPetSnapshot pet) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _PetSelectionCard(
                            pet: pet,
                            selected: _selectedPetIds.contains(pet.petId),
                            onChanged: (bool value) {
                              setState(() {
                                if (value) {
                                  _selectedPetIds.add(pet.petId);
                                } else {
                                  _selectedPetIds.remove(pet.petId);
                                }
                              });
                            },
                          ),
                        ),
                      )
                      .toList(),
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: FilledButton(
          onPressed: _isSubmitting ? null : _submit,
          child: Text(_isSubmitting ? '创建中...' : '创建邀请'),
        ),
      ),
    );
  }
}

class _InvitationHeroCard extends StatelessWidget {
  const _InvitationHeroCard({required this.sharedPetCount});

  final int sharedPetCount;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFAF4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '家庭邀请',
            icon: Icons.mail_outline_rounded,
            backgroundColor: Color(0xFFFFE2D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            '邀请成员',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          CompanionPill(
            label: '可共享宠物 $sharedPetCount 只',
            backgroundColor: AppThemePalette.surface,
          ),
        ],
      ),
    );
  }
}

class _PetSelectionCard extends StatelessWidget {
  const _PetSelectionCard({
    required this.pet,
    required this.selected,
    required this.onChanged,
  });

  final FamilySharedPetSnapshot pet;
  final bool selected;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(14),
      color: selected ? const Color(0xFFFFEEE4) : AppThemePalette.surfaceRaised,
      radius: 22,
      child: InkWell(
        onTap: () => onChanged(!selected),
        borderRadius: BorderRadius.circular(18),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: selected
                    ? const Color(0xFFFFDFC8)
                    : const Color(0xFFE8F3E7),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(
                pet.petType == 'dog'
                    ? Icons.pets_rounded
                    : Icons.cruelty_free_outlined,
                color: selected
                    ? AppThemePalette.primaryDeep
                    : AppThemePalette.success,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    pet.petName,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
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
            Checkbox(
              value: selected,
              onChanged: (bool? value) {
                if (value == null) {
                  return;
                }
                onChanged(value);
              },
            ),
          ],
        ),
      ),
    );
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
