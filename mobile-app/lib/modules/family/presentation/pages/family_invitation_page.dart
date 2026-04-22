import 'package:flutter/material.dart';
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
      appBar: AppBar(title: const Text('邀请家庭成员')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _InvitationSection(
              title: '邀请对象',
              description: '当前先支持手机号邀请，后续再扩展分享链接和二维码。',
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
            _InvitationSection(
              title: '邀请角色',
              description: '普通成员适合协助照护，管理员额外具备成员管理能力。',
              child: DropdownButtonFormField<String>(
                value: _role,
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
            _InvitationSection(
              title: '共享宠物',
              description: '邀请时必须明确共享哪些宠物，避免成员加入后看到超出预期的数据。',
              child: Column(
                children: widget.sharedPets
                    .map(
                      (FamilySharedPetSnapshot pet) => CheckboxListTile(
                        value: _selectedPetIds.contains(pet.petId),
                        contentPadding: EdgeInsets.zero,
                        title: Text(pet.petName),
                        subtitle: Text(
                            '${_toLocalizedPetType(pet.petType)} · ${pet.breed}'),
                        onChanged: (bool? value) {
                          setState(() {
                            if (value == true) {
                              _selectedPetIds.add(pet.petId);
                            } else {
                              _selectedPetIds.remove(pet.petId);
                            }
                          });
                        },
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
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

class _InvitationSection extends StatelessWidget {
  const _InvitationSection({
    required this.title,
    required this.description,
    required this.child,
  });

  final String title;
  final String description;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 6),
          Text(
            description,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
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
