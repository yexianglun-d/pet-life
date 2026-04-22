import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物表单页。
///
/// 创建与编辑共用同一页面，保证字段口径、校验规则和请求结构保持一致，
/// 避免两套页面长期演化后出现行为分叉。
class PetEditorPage extends StatefulWidget {
  const PetEditorPage.create({super.key}) : pet = null;

  const PetEditorPage.edit({
    super.key,
    required this.pet,
  });

  final PetDetailSnapshot? pet;

  bool get isEditMode => pet != null;

  @override
  State<PetEditorPage> createState() => _PetEditorPageState();
}

class _PetEditorPageState extends State<PetEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _petNameController;
  late final TextEditingController _breedController;
  late final TextEditingController _birthdayController;
  late final TextEditingController _adoptDateController;
  late String _petType;
  late String _gender;
  late String _neuterStatus;
  DateTime? _birthday;
  DateTime? _adoptDate;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    final PetDetailSnapshot? pet = widget.pet;
    _petNameController = TextEditingController(text: pet?.petName ?? '');
    _breedController = TextEditingController(text: pet?.breed ?? '');
    _birthday = pet?.birthday;
    _adoptDate = pet?.adoptDate;
    _birthdayController =
        TextEditingController(text: _formatDateLabel(_birthday));
    _adoptDateController =
        TextEditingController(text: _formatDateLabel(_adoptDate));
    _petType = pet?.petType ?? 'cat';
    _gender = pet?.gender ?? 'female';
    _neuterStatus = pet?.neuterStatus == 'unknown'
        ? 'pending'
        : (pet?.neuterStatus ?? 'pending');
  }

  @override
  void dispose() {
    _petNameController.dispose();
    _breedController.dispose();
    _birthdayController.dispose();
    _adoptDateController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isSubmitting || !_formKey.currentState!.validate()) {
      return;
    }

    if (_birthday != null &&
        _adoptDate != null &&
        _adoptDate!.isBefore(_birthday!)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('到家日期不能早于生日')),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final PetUpsertDraft draft = PetUpsertDraft(
        petName: _petNameController.text.trim(),
        petType: _petType,
        breed: _breedController.text.trim(),
        gender: _gender,
        neuterStatus: _neuterStatus,
        birthday: _birthday,
        adoptDate: _adoptDate,
      );
      final repository = PetLifeAppScope.repositoryOf(context);
      if (widget.isEditMode) {
        await repository.updatePet(
          petId: widget.pet!.petId,
          draft: draft,
        );
      } else {
        await repository.createPet(draft);
      }

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

  Future<void> _pickBirthday() async {
    final DateTime now = DateTime.now();
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _birthday ?? DateTime(now.year - 1, now.month, now.day),
      firstDate: DateTime(2000, 1, 1),
      lastDate: now,
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    setState(() {
      _birthday = selectedDate;
      _birthdayController.text = _formatDateLabel(selectedDate);
    });
  }

  Future<void> _pickAdoptDate() async {
    final DateTime now = DateTime.now();
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _adoptDate ?? _birthday ?? now,
      firstDate: DateTime(2000, 1, 1),
      lastDate: now,
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    setState(() {
      _adoptDate = selectedDate;
      _adoptDateController.text = _formatDateLabel(selectedDate);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isEditMode ? '编辑宠物' : '新建宠物'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _FormSection(
              title: '基础信息',
              description: '先补齐宠物主档的核心字段，保证首页、提醒和日常都能基于同一只宠物运转。',
              child: Column(
                children: [
                  TextFormField(
                    controller: _petNameController,
                    decoration: const InputDecoration(
                      labelText: '宠物名称',
                      hintText: '例如：Momo',
                    ),
                    validator: (String? value) {
                      return value == null || value.trim().isEmpty
                          ? '请输入宠物名称'
                          : null;
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _petType,
                    decoration: const InputDecoration(labelText: '宠物类型'),
                    items: const [
                      DropdownMenuItem(value: 'cat', child: Text('猫')),
                      DropdownMenuItem(value: 'dog', child: Text('狗')),
                      DropdownMenuItem(value: 'other', child: Text('其他')),
                    ],
                    onChanged: (String? value) {
                      if (value == null) {
                        return;
                      }
                      setState(() {
                        _petType = value;
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _breedController,
                    decoration: const InputDecoration(
                      labelText: '品种',
                      hintText: '例如：British Shorthair',
                    ),
                    validator: (String? value) {
                      return value == null || value.trim().isEmpty
                          ? '请输入品种'
                          : null;
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _gender,
                    decoration: const InputDecoration(labelText: '性别'),
                    items: const [
                      DropdownMenuItem(value: 'female', child: Text('母')),
                      DropdownMenuItem(value: 'male', child: Text('公')),
                    ],
                    onChanged: (String? value) {
                      if (value == null) {
                        return;
                      }
                      setState(() {
                        _gender = value;
                      });
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _FormSection(
              title: '补充信息',
              description: '日期与绝育状态会影响宠物主页展示和后续提醒配置，先在主档层统一维护。',
              child: Column(
                children: [
                  TextFormField(
                    controller: _birthdayController,
                    readOnly: true,
                    decoration: const InputDecoration(
                      labelText: '生日',
                      hintText: '请选择生日',
                      suffixIcon: Icon(Icons.calendar_today_outlined),
                    ),
                    onTap: _pickBirthday,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _adoptDateController,
                    readOnly: true,
                    decoration: const InputDecoration(
                      labelText: '到家日期',
                      hintText: '请选择到家日期',
                      suffixIcon: Icon(Icons.calendar_today_outlined),
                    ),
                    onTap: _pickAdoptDate,
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _neuterStatus,
                    decoration: const InputDecoration(labelText: '绝育状态'),
                    items: const [
                      DropdownMenuItem(value: 'pending', child: Text('未完成')),
                      DropdownMenuItem(value: 'completed', child: Text('已完成')),
                      DropdownMenuItem(value: 'unknown', child: Text('暂不确定')),
                    ],
                    onChanged: (String? value) {
                      if (value == null) {
                        return;
                      }
                      setState(() {
                        _neuterStatus = value;
                      });
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: FilledButton(
          onPressed: _isSubmitting ? null : _submit,
          child: Text(
              _isSubmitting ? '保存中...' : (widget.isEditMode ? '保存修改' : '创建宠物')),
        ),
      ),
    );
  }
}

class _FormSection extends StatelessWidget {
  const _FormSection({
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

String _formatDateLabel(DateTime? value) {
  if (value == null) {
    return '';
  }

  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  return '${value.year}-$month-$day';
}
