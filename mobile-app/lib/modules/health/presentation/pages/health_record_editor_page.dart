import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/media_attachment_picker.dart';
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
  late final TextEditingController _hospitalController;
  late final TextEditingController _doctorController;
  late final TextEditingController _resultSummaryController;
  late final TextEditingController _notesController;
  late final TextEditingController _occurredAtController;
  late final TextEditingController _nextReminderAtController;
  late final TextEditingController _nextReminderTitleController;
  late String _recordType;
  late String _severityLevel;
  late DateTime _occurredAt;
  late List<String> _attachmentAssetIds;
  DateTime? _nextReminderAt;
  bool _createNextReminder = false;
  bool _isSubmitting = false;
  bool _isUploadingAttachments = false;
  bool _hasFailedAttachments = false;
  String? _formNoticeMessage;

  @override
  void initState() {
    super.initState();
    final HealthRecordSnapshot? initialRecord = widget.initialRecord;
    _titleController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.title,
    );
    _valueController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.value ?? '',
    );
    _unitController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.unit ?? '',
    );
    _hospitalController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.hospitalName ?? '',
    );
    _doctorController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.doctorName ?? '',
    );
    _resultSummaryController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.resultSummary ?? '',
    );
    _notesController = TextEditingController(
      text: initialRecord == null ? '' : initialRecord.notes ?? '',
    );
    _recordType = initialRecord?.recordType ?? 'vaccine';
    _severityLevel = initialRecord?.severityLevel ?? 'mild';
    _occurredAt = initialRecord?.occurredAt ?? DateTime.now();
    _attachmentAssetIds =
        List<String>.of(initialRecord?.attachmentAssetIds ?? const <String>[]);
    _nextReminderAt = initialRecord?.nextReminderAt;
    _createNextReminder = _nextReminderAt != null;
    _occurredAtController =
        TextEditingController(text: _formatOccurredAtLabel(_occurredAt));
    _nextReminderAtController = TextEditingController(
      text: _nextReminderAt == null
          ? ''
          : _formatOccurredAtLabel(_nextReminderAt!),
    );
    _nextReminderTitleController = TextEditingController();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _valueController.dispose();
    _unitController.dispose();
    _hospitalController.dispose();
    _doctorController.dispose();
    _resultSummaryController.dispose();
    _notesController.dispose();
    _occurredAtController.dispose();
    _nextReminderAtController.dispose();
    _nextReminderTitleController.dispose();
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

  Future<void> _pickNextReminderAt() async {
    final DateTime initialDate =
        _nextReminderAt ?? DateTime.now().add(const Duration(days: 30));
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 3650)),
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    final TimeOfDay? selectedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initialDate),
    );
    if (!mounted) {
      return;
    }

    final TimeOfDay resolvedTime =
        selectedTime ?? TimeOfDay.fromDateTime(initialDate);
    final DateTime selectedDateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      resolvedTime.hour,
      resolvedTime.minute,
    );

    setState(() {
      _nextReminderAt = selectedDateTime;
      _nextReminderAtController.text = _formatOccurredAtLabel(selectedDateTime);
    });
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }
    if (!_formKey.currentState!.validate()) {
      _showFormNotice('还有健康记录信息没有填完整，请先看标红的输入框。');
      return;
    }
    if (_isUploadingAttachments) {
      _showFormNotice('附件还在上传中，请稍后再保存。');
      return;
    }
    if (_hasFailedAttachments) {
      _showFormNotice('请先移除上传失败的附件。');
      return;
    }
    if (_createNextReminder && _nextReminderAt == null) {
      _showFormNotice('请选择下一次提醒时间。');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _formNoticeMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final HealthRecordDraft draft = HealthRecordDraft(
        recordType: _recordType,
        title: _titleController.text.trim(),
        occurredAt: _occurredAt,
        value: _normalizeNullableField(_valueController.text),
        unit: _normalizeNullableField(_unitController.text),
        hospitalName: _normalizeNullableField(_hospitalController.text),
        doctorName: _normalizeNullableField(_doctorController.text),
        severityLevel: _supportsSeverity(_recordType) ? _severityLevel : null,
        resultSummary: _normalizeNullableField(_resultSummaryController.text),
        nextReminderAt: _createNextReminder ? _nextReminderAt : null,
        nextReminderTitle: _createNextReminder
            ? _normalizeNullableField(_nextReminderTitleController.text)
            : null,
        attachmentAssetIds: _attachmentAssetIds,
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
      showCompanionSuccessFeedback(
        context,
        widget.initialRecord == null ? '健康记录已保存' : '健康记录已更新',
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

  @override
  Widget build(BuildContext context) {
    final bool isEditMode = widget.initialRecord != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditMode ? '编辑健康记录' : '新建健康记录'),
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
              _EditorHeroCard(
                title: isEditMode ? '调整一条健康记录' : '留下新的健康记录',
                description: isEditMode
                    ? '把这次补充的信息补完整，后面回看会更清楚。'
                    : '把重要的健康变化记下来，以后回看会很有帮助。',
              ),
              if (_formNoticeMessage != null) ...[
                const SizedBox(height: 12),
                CompanionFormNotice(message: _formNoticeMessage!),
              ],
              const SizedBox(height: 16),
              _FormSection(
                title: '记录类型',
                description: '先选这次属于哪类健康事件，后面整理时会更清楚。',
                child: DropdownButtonFormField<String>(
                  value: _recordType,
                  decoration: const InputDecoration(labelText: '记录类型'),
                  items: const [
                    DropdownMenuItem(value: 'vaccine', child: Text('疫苗')),
                    DropdownMenuItem(value: 'deworming', child: Text('驱虫')),
                    DropdownMenuItem(value: 'examination', child: Text('体检')),
                    DropdownMenuItem(value: 'medication', child: Text('用药')),
                    DropdownMenuItem(value: 'weight', child: Text('体重')),
                    DropdownMenuItem(value: 'observation', child: Text('异常观察')),
                  ],
                  onChanged: (String? value) {
                    if (value == null) {
                      return;
                    }
                    _handleRecordTypeChanged(value);
                  },
                ),
              ),
              const SizedBox(height: 16),
              _FormSection(
                title: '记录内容',
                description: '标题、时间和补充说明越清楚，以后回看时越有价值。',
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
                    TextFormField(
                      controller: _notesController,
                      minLines: 3,
                      maxLines: 5,
                      decoration: const InputDecoration(
                        labelText: '备注',
                        hintText: '例如医院、药量、观察结果和当时状态',
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              _FormSection(
                title: _specializedSectionTitle(_recordType),
                description: _specializedSectionDescription(_recordType),
                child: _buildSpecializedFields(),
              ),
              const SizedBox(height: 16),
              _FormSection(
                title: '附件',
                description: '把检查单、化验结果或处方一起放进记录里，复诊时更容易核对。',
                child: MediaAttachmentPicker(
                  bizType: 'health_report',
                  initialAssetIds: _attachmentAssetIds,
                  allowedExtensions: const [
                    'jpg',
                    'jpeg',
                    'png',
                    'webp',
                    'gif',
                    'pdf',
                  ],
                  maxItems: 6,
                  pickButtonLabel: '添加图片或 PDF',
                  emptyDescription: '可以上传检查单、处方、体检报告或现场照片。',
                  onSelectionChanged: _handleAttachmentSelectionChanged,
                ),
              ),
              if (_supportsNextReminder(_recordType)) ...[
                const SizedBox(height: 16),
                _FormSection(
                  title: '下一次提醒',
                  description: '疫苗、驱虫和体检通常都有复查或下一次安排，可以直接生成提醒。',
                  child: _buildNextReminderFields(),
                ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: FilledButton(
          onPressed: _isSubmitting || _isUploadingAttachments ? null : _submit,
          child: Text(
            _isSubmitting
                ? '保存中...'
                : _isUploadingAttachments
                    ? '附件上传中...'
                    : isEditMode
                        ? '保存修改'
                        : '保存记录',
          ),
        ),
      ),
    );
  }

  void _handleAttachmentSelectionChanged(
    MediaAttachmentSelectionState selectionState,
  ) {
    setState(() {
      _attachmentAssetIds = selectionState.assetIds;
      _isUploadingAttachments = selectionState.isUploading;
      _hasFailedAttachments = selectionState.hasFailed;
    });
  }

  void _handleRecordTypeChanged(String value) {
    setState(() {
      _recordType = value;
      if (!_supportsNextReminder(value)) {
        _createNextReminder = false;
        _nextReminderAt = null;
        _nextReminderAtController.clear();
        _nextReminderTitleController.clear();
      }
      if (value == 'weight' && _unitController.text.trim().isEmpty) {
        _unitController.text = 'kg';
      }
      if (!_supportsSeverity(value)) {
        _severityLevel = 'mild';
      }
    });
  }

  Widget _buildSpecializedFields() {
    if (_recordType == 'weight' || _recordType == 'medication') {
      return Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _valueController,
                  decoration: InputDecoration(
                    labelText: _recordType == 'weight' ? '体重数值' : '用药剂量',
                    hintText: _recordType == 'weight' ? '例如 4.6' : '例如 2',
                  ),
                  validator: _recordType == 'weight'
                      ? (String? value) {
                          return value == null || value.trim().isEmpty
                              ? '请输入体重数值'
                              : null;
                        }
                      : null,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _unitController,
                  decoration: InputDecoration(
                    labelText: _recordType == 'weight' ? '体重单位' : '剂量单位',
                    hintText: _recordType == 'weight' ? 'kg' : '滴 / 片 / ml',
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _resultSummaryController,
            decoration: InputDecoration(
              labelText: _recordType == 'weight' ? '体重状态' : '用药结果',
              hintText: _recordType == 'weight' ? '例如 状态稳定' : '例如 已按医嘱完成',
            ),
          ),
        ],
      );
    }

    if (_supportsClinicalFields(_recordType)) {
      return Column(
        children: [
          TextFormField(
            controller: _hospitalController,
            decoration: const InputDecoration(
              labelText: '医院 / 机构',
              hintText: '例如 安心宠物医院',
            ),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _doctorController,
            decoration: const InputDecoration(
              labelText: '医生',
              hintText: '可选',
            ),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _resultSummaryController,
            decoration: InputDecoration(
              labelText: _recordType == 'vaccine'
                  ? '接种结果'
                  : _recordType == 'deworming'
                      ? '驱虫结果'
                      : '检查结果',
              hintText: '例如 已完成，无明显异常',
            ),
          ),
        ],
      );
    }

    return Column(
      children: [
        DropdownButtonFormField<String>(
          value: _severityLevel,
          decoration: const InputDecoration(labelText: '严重程度'),
          items: const [
            DropdownMenuItem(value: 'mild', child: Text('轻微')),
            DropdownMenuItem(value: 'medium', child: Text('中等')),
            DropdownMenuItem(value: 'severe', child: Text('严重')),
          ],
          onChanged: (String? value) {
            if (value == null) {
              return;
            }
            setState(() {
              _severityLevel = value;
            });
          },
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _resultSummaryController,
          decoration: const InputDecoration(
            labelText: '观察结果',
            hintText: '例如 已恢复食欲 / 仍需观察',
          ),
        ),
      ],
    );
  }

  Widget _buildNextReminderFields() {
    return Column(
      children: [
        SwitchListTile.adaptive(
          value: _createNextReminder,
          contentPadding: EdgeInsets.zero,
          title: const Text('生成下一次提醒'),
          onChanged: (bool value) {
            setState(() {
              _createNextReminder = value;
            });
          },
        ),
        if (_createNextReminder) ...[
          const SizedBox(height: 16),
          TextFormField(
            controller: _nextReminderAtController,
            readOnly: true,
            decoration: const InputDecoration(
              labelText: '提醒时间',
              suffixIcon: Icon(Icons.schedule_outlined),
            ),
            onTap: _pickNextReminderAt,
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _nextReminderTitleController,
            decoration: InputDecoration(
              labelText: '提醒标题',
              hintText:
                  '默认：下次${_titleController.text.trim().isEmpty ? _defaultTitleHint(_recordType) : _titleController.text.trim()}',
            ),
          ),
        ],
      ],
    );
  }
}

class _EditorHeroCard extends StatelessWidget {
  const _EditorHeroCard({
    required this.title,
    required this.description,
  });

  final String title;
  final String description;

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
            label: '健康档案编辑',
            icon: Icons.edit_note_rounded,
            backgroundColor: Color(0xFFFFE1CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(title, style: Theme.of(context).textTheme.headlineSmall),
        ],
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
    return CompanionCard(
      padding: const EdgeInsets.all(18),
      color: AppThemePalette.surface,
      radius: 24,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
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
    case 'weight':
      return '例如：今日体重';
    case 'observation':
      return '例如：呕吐观察';
    default:
      return '请输入记录标题';
  }
}

String _specializedSectionTitle(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return '疫苗信息';
    case 'deworming':
      return '驱虫信息';
    case 'examination':
      return '体检信息';
    case 'medication':
      return '用药信息';
    case 'weight':
      return '体重信息';
    case 'observation':
      return '观察信息';
    default:
      return '补充信息';
  }
}

String _specializedSectionDescription(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return '记录接种机构、医生和接种结果，后续复查时更清楚。';
    case 'deworming':
      return '把驱虫方式、执行结果和下一次安排整理好。';
    case 'examination':
      return '体检结果通常信息较多，建议补充机构、医生和结论。';
    case 'medication':
      return '记录剂量、单位和用药后的状态。';
    case 'weight':
      return '体重记录需要明确数值和单位，方便观察长期变化。';
    case 'observation':
      return '异常观察需要标明严重程度和当前结果。';
    default:
      return '补充这条健康记录的关键信息。';
  }
}

bool _supportsClinicalFields(String recordType) {
  return recordType == 'vaccine' ||
      recordType == 'deworming' ||
      recordType == 'examination';
}

bool _supportsNextReminder(String recordType) {
  return _supportsClinicalFields(recordType);
}

bool _supportsSeverity(String recordType) {
  return recordType == 'observation';
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
