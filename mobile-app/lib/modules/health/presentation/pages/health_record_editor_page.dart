import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/health_record_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 健康记录新建/编辑页。
class HealthRecordEditorPage extends StatefulWidget {
  const HealthRecordEditorPage({
    super.key,
    required this.petId,
    this.initialRecord,
  });

  final String petId;
  final HealthRecordSnapshot? initialRecord;

  @override
  State<HealthRecordEditorPage> createState() => _HealthRecordEditorPageState();
}

class _HealthRecordEditorPageState extends State<HealthRecordEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _valueController;
  late final TextEditingController _unitController;
  late final TextEditingController _notesController;
  late final TextEditingController _occurredAtController;
  late String _recordType;
  late DateTime _occurredAt;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    final HealthRecordSnapshot? initialRecord = widget.initialRecord;
    _titleController = TextEditingController(
        text: initialRecord == null ? '' : initialRecord.title);
    _valueController = TextEditingController(
        text: initialRecord == null ? '' : initialRecord.value ?? '');
    _unitController = TextEditingController(
        text: initialRecord == null ? '' : initialRecord.unit ?? '');
    _notesController = TextEditingController(
        text: initialRecord == null ? '' : initialRecord.notes ?? '');
    _recordType = initialRecord?.recordType ?? 'vaccine';
    _occurredAt = initialRecord?.occurredAt ?? DateTime.now();
    _occurredAtController =
        TextEditingController(text: _formatOccurredAtLabel(_occurredAt));
  }

  @override
  void dispose() {
    _titleController.dispose();
    _valueController.dispose();
    _unitController.dispose();
    _notesController.dispose();
    _occurredAtController.dispose();
    super.dispose();
  }

  Future<void> _pickOccurredAt() async {
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _occurredAt,
      firstDate: DateTime(2000, 1, 1),
      lastDate: DateTime.now(),
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    final TimeOfDay? selectedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_occurredAt),
    );
    if (!mounted) {
      return;
    }

    final TimeOfDay resolvedTime =
        selectedTime ?? TimeOfDay.fromDateTime(_occurredAt);
    final DateTime selectedDateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      resolvedTime.hour,
      resolvedTime.minute,
    );

    setState(() {
      _occurredAt = selectedDateTime;
      _occurredAtController.text = _formatOccurredAtLabel(selectedDateTime);
    });
  }

  Future<void> _submit() async {
    if (_isSubmitting || !_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final HealthRecordDraft draft = HealthRecordDraft(
        recordType: _recordType,
        title: _titleController.text.trim(),
        occurredAt: _occurredAt,
        value: _normalizeNullableField(_valueController.text),
        unit: _normalizeNullableField(_unitController.text),
        notes: _normalizeNullableField(_notesController.text),
      );
      if (widget.initialRecord == null) {
        await repository.createHealthRecord(
          petId: widget.petId,
          draft: draft,
        );
      } else {
        await repository.updateHealthRecord(
          petId: widget.petId,
          healthRecordId: widget.initialRecord!.healthRecordId,
          draft: draft,
        );
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

  @override
  Widget build(BuildContext context) {
    final bool isEditMode = widget.initialRecord != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditMode ? '编辑健康记录' : '新建健康记录'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _FormSection(
              title: '记录类型',
              description: '统一入口支持疫苗、驱虫、体检、用药和异常观察，避免用户为了不同记录来回切换流程。',
              child: DropdownButtonFormField<String>(
                value: _recordType,
                decoration: const InputDecoration(labelText: '记录类型'),
                items: const [
                  DropdownMenuItem(value: 'vaccine', child: Text('疫苗')),
                  DropdownMenuItem(value: 'deworming', child: Text('驱虫')),
                  DropdownMenuItem(value: 'examination', child: Text('体检')),
                  DropdownMenuItem(value: 'medication', child: Text('用药')),
                  DropdownMenuItem(value: 'observation', child: Text('异常观察')),
                ],
                onChanged: (String? value) {
                  if (value == null) {
                    return;
                  }
                  setState(() {
                    _recordType = value;
                  });
                },
              ),
            ),
            const SizedBox(height: 16),
            _FormSection(
              title: '记录内容',
              description: '标题和发生时间是健康档案的核心索引字段，数值与单位作为补充信息保留。',
              child: Column(
                children: [
                  TextFormField(
                    controller: _titleController,
                    decoration: InputDecoration(
                      labelText: '记录标题',
                      hintText: _defaultTitleHint(_recordType),
                    ),
                    validator: (String? value) {
                      return value == null || value.trim().isEmpty
                          ? '请输入记录标题'
                          : null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _occurredAtController,
                    readOnly: true,
                    decoration: const InputDecoration(
                      labelText: '发生时间',
                      suffixIcon: Icon(Icons.schedule_outlined),
                    ),
                    onTap: _pickOccurredAt,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _valueController,
                          decoration: const InputDecoration(
                            labelText: '数值',
                            hintText: '可选',
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextFormField(
                          controller: _unitController,
                          decoration: const InputDecoration(
                            labelText: '单位',
                            hintText: '可选',
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _notesController,
                    minLines: 3,
                    maxLines: 5,
                    decoration: const InputDecoration(
                      labelText: '备注',
                      hintText: '可记录医院、药量、观察结果等信息',
                    ),
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
            _isSubmitting
                ? '保存中...'
                : isEditMode
                    ? '保存修改'
                    : '保存记录',
          ),
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

String _defaultTitleHint(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return '例如：狂犬疫苗';
    case 'deworming':
      return '例如：体内驱虫';
    case 'examination':
      return '例如：年度体检';
    case 'medication':
      return '例如：耳螨滴药';
    case 'observation':
      return '例如：呕吐观察';
    default:
      return '请输入记录标题';
  }
}

String _formatOccurredAtLabel(DateTime occurredAt) {
  final String month = occurredAt.month.toString().padLeft(2, '0');
  final String day = occurredAt.day.toString().padLeft(2, '0');
  final String hour = occurredAt.hour.toString().padLeft(2, '0');
  final String minute = occurredAt.minute.toString().padLeft(2, '0');
  return '${occurredAt.year}-$month-$day $hour:$minute';
}

String? _normalizeNullableField(String value) {
  final String normalizedValue = value.trim();
  return normalizedValue.isEmpty ? null : normalizedValue;
}
