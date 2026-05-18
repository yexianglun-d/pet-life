import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物表单页。
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
  late final TextEditingController _weightController;
  late final TextEditingController _allergyNotesController;
  late final TextEditingController _medicalHistoryController;
  late final TextEditingController _birthdayController;
  late final TextEditingController _adoptDateController;
  late String _petType;
  late String _gender;
  late String _neuterStatus;
  DateTime? _birthday;
  DateTime? _adoptDate;
  bool _isSubmitting = false;
  String? _formNoticeMessage;

  @override
  void initState() {
    super.initState();
    final PetDetailSnapshot? pet = widget.pet;
    _petNameController = TextEditingController(text: pet?.petName ?? '');
    _breedController = TextEditingController(text: pet?.breed ?? '');
    _weightController = TextEditingController(text: pet?.weightKg ?? '');
    _allergyNotesController =
        TextEditingController(text: pet?.allergyNotes ?? '');
    _medicalHistoryController =
        TextEditingController(text: pet?.medicalHistory ?? '');
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
    _weightController.dispose();
    _allergyNotesController.dispose();
    _medicalHistoryController.dispose();
    _birthdayController.dispose();
    _adoptDateController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }
    if (!_formKey.currentState!.validate()) {
      _showFormNotice('还有宠物档案信息没有填完整，请先看标红的输入框。');
      return;
    }

    if (_birthday != null &&
        _adoptDate != null &&
        _adoptDate!.isBefore(_birthday!)) {
      _showFormNotice('到家日期不能早于生日，请重新确认这两个日期。');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _formNoticeMessage = null;
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
        weightKg: _weightController.text.trim(),
        allergyNotes: _allergyNotesController.text.trim(),
        medicalHistory: _medicalHistoryController.text.trim(),
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
      showCompanionSuccessFeedback(
        context,
        widget.isEditMode ? '宠物档案已更新' : '宠物档案已创建',
      );
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  void _showFormNotice(String message) {
    setState(() {
      _formNoticeMessage = message;
    });
  }

  String? _validateWeight(String? value) {
    final String weightText = value?.trim() ?? '';
    if (weightText.isEmpty) {
      return null;
    }
    final double? weightValue =
        double.tryParse(weightText.replaceAll(',', '.'));
    if (weightValue == null || weightValue <= 0) {
      return '体重请填写大于 0 的数字';
    }
    return null;
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
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _PetEditorHeroCard(isEditMode: widget.isEditMode),
              if (_formNoticeMessage != null) ...[
                const SizedBox(height: 12),
                CompanionFormNotice(message: _formNoticeMessage!),
              ],
              const SizedBox(height: 16),
              _ProfilePreviewCard(
                petName: _petNameController.text.trim(),
                petType: _petType,
                breed: _breedController.text.trim(),
                gender: _gender,
                birthday: _birthday,
                adoptDate: _adoptDate,
                weightKg: _weightController.text.trim(),
              ),
              const SizedBox(height: 16),
              _FormSection(
                title: '基础信息',
                description: '先把名字、类型和品种写清楚，这份主档会成为以后所有记录的起点。',
                child: Column(
                  children: [
                    TextFormField(
                      controller: _petNameController,
                      onChanged: (_) => setState(() {}),
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
                        DropdownMenuItem(value: 'cat', child: Text('猫咪')),
                        DropdownMenuItem(value: 'dog', child: Text('狗狗')),
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
                      onChanged: (_) => setState(() {}),
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
                description: '把重要日期和照护状态慢慢补齐，后续提醒与展示会更贴近真实陪伴。',
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
                        DropdownMenuItem(
                            value: 'completed', child: Text('已完成')),
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
              const SizedBox(height: 16),
              _FormSection(
                title: '照护备注',
                description: '补一点体重、过敏信息和病史，后面做提醒和服务推荐时会更贴近真实情况。',
                child: Column(
                  children: [
                    TextFormField(
                      controller: _weightController,
                      onChanged: (_) => setState(() {}),
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      decoration: const InputDecoration(
                        labelText: '当前体重（kg）',
                        hintText: '例如：4.6',
                      ),
                      validator: _validateWeight,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _allergyNotesController,
                      onChanged: (_) => setState(() {}),
                      minLines: 2,
                      maxLines: 4,
                      decoration: const InputDecoration(
                        labelText: '过敏信息',
                        hintText: '例如：对鸡肉和海鲜较敏感',
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _medicalHistoryController,
                      onChanged: (_) => setState(() {}),
                      minLines: 3,
                      maxLines: 5,
                      decoration: const InputDecoration(
                        labelText: '重要病史',
                        hintText: '把需要长期记住的诊疗和病史写在这里',
                      ),
                    ),
                  ],
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
          child: Text(
            _isSubmitting ? '保存中...' : (widget.isEditMode ? '保存修改' : '创建宠物'),
          ),
        ),
      ),
    );
  }
}

class _PetEditorHeroCard extends StatelessWidget {
  const _PetEditorHeroCard({required this.isEditMode});

  final bool isEditMode;

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
            label: '宠物主档编辑',
            icon: Icons.edit_note_rounded,
            backgroundColor: Color(0xFFFFE1CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            isEditMode ? '把这份档案补得更完整些' : '先建立毛孩子的第一份档案',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '名字、基础资料和重要日期整理清楚后，健康记录、提醒和成长时间轴都会更顺。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

class _ProfilePreviewCard extends StatelessWidget {
  const _ProfilePreviewCard({
    required this.petName,
    required this.petType,
    required this.breed,
    required this.gender,
    required this.birthday,
    required this.adoptDate,
    required this.weightKg,
  });

  final String petName;
  final String petType;
  final String breed;
  final String gender;
  final DateTime? birthday;
  final DateTime? adoptDate;
  final String weightKg;

  @override
  Widget build(BuildContext context) {
    final String displayName = petName.isEmpty ? '还没起名字的小可爱' : petName;
    final String displayBreed = breed.isEmpty ? '品种待补充' : breed;

    return CompanionCard(
      padding: const EdgeInsets.all(18),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '档案预览',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 6),
          Text(
            '边填写边看看这份主档现在长什么样。',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Icon(
                  petType == 'dog'
                      ? Icons.pets_rounded
                      : Icons.cruelty_free_outlined,
                  color: AppThemePalette.primaryDeep,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(displayName,
                        style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 4),
                    Text(
                      '${_toLocalizedPetType(petType)} · $displayBreed · ${_toLocalizedGender(gender)}',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppThemePalette.muted,
                          ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (birthday != null)
                CompanionPill(
                  label: '生日 ${_formatDateLabel(birthday)}',
                  backgroundColor: AppThemePalette.surface,
                ),
              if (adoptDate != null)
                CompanionPill(
                  label: '到家 ${_formatDateLabel(adoptDate)}',
                  backgroundColor: AppThemePalette.surface,
                ),
              if (weightKg.trim().isNotEmpty)
                CompanionPill(
                  label: '体重 ${weightKg.trim()} kg',
                  backgroundColor: AppThemePalette.surface,
                ),
              if (birthday == null && adoptDate == null)
                const CompanionPill(
                  label: '重要日期待补充',
                  backgroundColor: AppThemePalette.surface,
                ),
            ],
          ),
        ],
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
      return '其他';
  }
}

String _toLocalizedGender(String gender) {
  switch (gender) {
    case 'male':
      return '公';
    case 'female':
      return '母';
    default:
      return gender;
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
    return CompanionCard(
      padding: const EdgeInsets.all(18),
      radius: 24,
      color: AppThemePalette.surface,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 6),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
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
