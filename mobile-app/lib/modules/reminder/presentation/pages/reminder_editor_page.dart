import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_template_snapshot.dart';

/// 提醒新建页。
class ReminderEditorPage extends StatefulWidget {
  const ReminderEditorPage({
    super.key,
    required this.petId,
  });

  final String petId;

  @override
  State<ReminderEditorPage> createState() => _ReminderEditorPageState();
}

class _ReminderEditorPageState extends State<ReminderEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _notesController;
  late final TextEditingController _dueAtController;
  late final TextEditingController _cycleValueController;
  late String _reminderType;
  late String _reminderMode;
  late String _cycleUnit;
  late DateTime _dueAt;
  bool _didLoadTemplates = false;
  bool _isTemplateLoading = false;
  bool _isSubmitting = false;
  String? _selectedTemplateId;
  String? _templateErrorMessage;
  String? _formNoticeMessage;
  List<ReminderTemplateSnapshot> _reminderTemplates =
      const <ReminderTemplateSnapshot>[];

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController();
    _notesController = TextEditingController();
    _reminderType = 'vaccine';
    _reminderMode = 'single';
    _cycleUnit = 'month';
    _dueAt = _buildDefaultDueAt();
    _dueAtController = TextEditingController(text: _formatDueAtLabel(_dueAt));
    _cycleValueController = TextEditingController(text: '1');
  }

  @override
  void dispose() {
    _titleController.dispose();
    _notesController.dispose();
    _dueAtController.dispose();
    _cycleValueController.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoadTemplates) {
      return;
    }

    _didLoadTemplates = true;
    _loadReminderTemplates();
  }

  Future<void> _loadReminderTemplates() async {
    setState(() {
      _isTemplateLoading = true;
      _templateErrorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<ReminderTemplateSnapshot> templates =
          await repository.listReminderTemplates(widget.petId);
      if (!mounted) {
        return;
      }

      setState(() {
        _reminderTemplates = templates;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        _templateErrorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isTemplateLoading = false;
        });
      }
    }
  }

  void _applyReminderTemplate(ReminderTemplateSnapshot template) {
    setState(() {
      _selectedTemplateId = template.templateId;
      _reminderType = template.reminderType;
      _reminderMode = template.defaultReminderMode;
      _titleController.text = template.templateName;
      if (template.defaultReminderMode == 'cycle') {
        _cycleValueController.text =
            (template.defaultCycleValue ?? 1).toString();
        _cycleUnit = template.defaultCycleUnit ?? 'month';
      }
      _formNoticeMessage = null;
    });
  }

  Future<void> _pickDueAt() async {
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _dueAt,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now().add(const Duration(days: 3650)),
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    final TimeOfDay? selectedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_dueAt),
    );
    if (!mounted) {
      return;
    }

    final TimeOfDay resolvedTime =
        selectedTime ?? TimeOfDay.fromDateTime(_dueAt);
    final DateTime selectedDateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      resolvedTime.hour,
      resolvedTime.minute,
    );

    setState(() {
      _dueAt = selectedDateTime;
      _dueAtController.text = _formatDueAtLabel(selectedDateTime);
    });
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }
    if (!_formKey.currentState!.validate()) {
      _showFormNotice('还有提醒信息没有填完整，请先看标红的输入框。');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _formNoticeMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.createReminder(
        petId: widget.petId,
        draft: ReminderDraft(
          reminderType: _reminderType,
          title: _titleController.text.trim(),
          reminderMode: _reminderMode,
          cycleValue: _reminderMode == 'cycle'
              ? int.tryParse(_cycleValueController.text.trim())
              : null,
          cycleUnit: _reminderMode == 'cycle' ? _cycleUnit : null,
          dueAt: _dueAt,
          notes: _normalizeNullableField(_notesController.text),
        ),
      );
      if (!mounted) {
        return;
      }

      showCompanionSuccessFeedback(context, '提醒已保存');
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
    return Scaffold(
      appBar: AppBar(title: const Text('新建提醒')),
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
              _ReminderEditorHeroCard(reminderMode: _reminderMode),
              if (_formNoticeMessage != null) ...[
                const SizedBox(height: 12),
                CompanionFormNotice(message: _formNoticeMessage!),
              ],
              const SizedBox(height: 16),
              _ReminderFormSection(
                title: '从模板开始',
                description: '后台维护的常用照护模板会出现在这里，选择后仍可以按实际情况调整。',
                child: _buildTemplateSelector(),
              ),
              const SizedBox(height: 16),
              _ReminderFormSection(
                title: '提醒类型',
                description: '先选这次想记住的是什么，再决定它是一次还是周期。',
                child: DropdownButtonFormField<String>(
                  value: _reminderType,
                  decoration: const InputDecoration(labelText: '提醒类型'),
                  items: const [
                    DropdownMenuItem(value: 'vaccine', child: Text('疫苗')),
                    DropdownMenuItem(value: 'deworming', child: Text('驱虫')),
                    DropdownMenuItem(value: 'examination', child: Text('体检')),
                    DropdownMenuItem(value: 'medication', child: Text('用药')),
                    DropdownMenuItem(value: 'custom', child: Text('自定义')),
                  ],
                  onChanged: (String? value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
                      _selectedTemplateId = null;
                      _reminderType = value;
                    });
                  },
                ),
              ),
              const SizedBox(height: 16),
              _ReminderFormSection(
                title: '提醒模式',
                description: '周期提醒适合驱虫、体检这类会重复发生的照护事项。',
                child: Column(
                  children: [
                    DropdownButtonFormField<String>(
                      value: _reminderMode,
                      decoration: const InputDecoration(labelText: '提醒模式'),
                      items: const [
                        DropdownMenuItem(value: 'single', child: Text('单次提醒')),
                        DropdownMenuItem(value: 'cycle', child: Text('周期提醒')),
                      ],
                      onChanged: (String? value) {
                        if (value == null) {
                          return;
                        }
                        setState(() {
                          _selectedTemplateId = null;
                          _reminderMode = value;
                        });
                      },
                    ),
                    if (_reminderMode == 'cycle') ...[
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: _cycleValueController,
                              keyboardType: TextInputType.number,
                              decoration: const InputDecoration(
                                labelText: '间隔值',
                                hintText: '例如 1',
                              ),
                              validator: (String? value) {
                                if (_reminderMode != 'cycle') {
                                  return null;
                                }
                                final int? cycleValue =
                                    int.tryParse((value ?? '').trim());
                                if (cycleValue == null || cycleValue <= 0) {
                                  return '请输入大于 0 的间隔值';
                                }
                                return null;
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: DropdownButtonFormField<String>(
                              value: _cycleUnit,
                              decoration:
                                  const InputDecoration(labelText: '周期单位'),
                              items: const [
                                DropdownMenuItem(
                                    value: 'day', child: Text('天')),
                                DropdownMenuItem(
                                    value: 'week', child: Text('周')),
                                DropdownMenuItem(
                                    value: 'month', child: Text('月')),
                              ],
                              onChanged: (String? value) {
                                if (value == null) {
                                  return;
                                }
                                setState(() {
                                  _selectedTemplateId = null;
                                  _cycleUnit = value;
                                });
                              },
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 16),
              _ReminderFormSection(
                title: '提醒内容',
                description: '时间、标题和备注越清楚，到点时看到的提示就越好理解。',
                child: Column(
                  children: [
                    TextFormField(
                      controller: _titleController,
                      decoration: InputDecoration(
                        labelText: '提醒标题',
                        hintText: _defaultTitleHint(_reminderType),
                      ),
                      validator: (String? value) {
                        return value == null || value.trim().isEmpty
                            ? '请输入提醒标题'
                            : null;
                      },
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _dueAtController,
                      readOnly: true,
                      decoration: const InputDecoration(
                        labelText: '提醒时间',
                        suffixIcon: Icon(Icons.schedule_outlined),
                      ),
                      onTap: _pickDueAt,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _notesController,
                      minLines: 3,
                      maxLines: 5,
                      decoration: const InputDecoration(
                        labelText: '备注',
                        hintText: '例如医院、药量、注意事项',
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
          child: Text(_isSubmitting ? '保存中...' : '保存提醒'),
        ),
      ),
    );
  }

  Widget _buildTemplateSelector() {
    if (_isTemplateLoading && _reminderTemplates.isEmpty) {
      return const _TemplateLoadingCard();
    }

    if (_templateErrorMessage != null && _reminderTemplates.isEmpty) {
      return CompanionEmptyState(
        title: '提醒模板暂时没有加载出来',
        description: _templateErrorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadReminderTemplates,
      );
    }

    if (_reminderTemplates.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有可用模板',
        description: '可以先手动填写提醒，后台启用模板后这里会自动出现。',
        icon: Icons.auto_awesome_outlined,
      );
    }

    return Column(
      children: _reminderTemplates
          .map(
            (ReminderTemplateSnapshot template) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _ReminderTemplateCard(
                template: template,
                selected: _selectedTemplateId == template.templateId,
                onTap: () => _applyReminderTemplate(template),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _ReminderEditorHeroCard extends StatelessWidget {
  const _ReminderEditorHeroCard({required this.reminderMode});

  final String reminderMode;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEBDD),
          Color(0xFFFFFBF5),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: reminderMode == 'cycle' ? '周期提醒' : '单次提醒',
            icon: Icons.alarm_add_rounded,
            backgroundColor: const Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            '把要记住的时间点先排好',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '到时间时你会更从容，不需要一直担心自己有没有漏掉。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

class _ReminderFormSection extends StatelessWidget {
  const _ReminderFormSection({
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
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}

class _TemplateLoadingCard extends StatelessWidget {
  const _TemplateLoadingCard();

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(16),
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(
              Icons.auto_awesome_rounded,
              color: AppThemePalette.primaryDeep,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              '正在整理常用提醒模板...',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}

class _ReminderTemplateCard extends StatelessWidget {
  const _ReminderTemplateCard({
    required this.template,
    required this.selected,
    required this.onTap,
  });

  final ReminderTemplateSnapshot template;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    return InkWell(
      borderRadius: BorderRadius.circular(22),
      onTap: onTap,
      child: CompanionCard(
        padding: const EdgeInsets.all(16),
        radius: 22,
        color:
            selected ? const Color(0xFFFFE7D8) : AppThemePalette.surfaceRaised,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    template.templateName,
                    style: textTheme.titleMedium,
                  ),
                ),
                Icon(
                  selected
                      ? Icons.check_circle_rounded
                      : Icons.radio_button_unchecked_rounded,
                  color: selected
                      ? AppThemePalette.primaryDeep
                      : AppThemePalette.muted,
                ),
              ],
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                CompanionPill(
                  label: _reminderTypeLabel(template.reminderType),
                  backgroundColor: AppThemePalette.warmTint,
                  foregroundColor: AppThemePalette.primaryDeep,
                ),
                CompanionPill(
                  label: template.defaultReminderMode == 'cycle'
                      ? _templateCycleLabel(template)
                      : '单次提醒',
                  backgroundColor: AppThemePalette.surface,
                ),
                CompanionPill(
                  label:
                      '提前 ${template.defaultAdvanceValue} ${_unitLabel(template.defaultAdvanceUnit)}',
                  backgroundColor: AppThemePalette.surface,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

DateTime _buildDefaultDueAt() {
  final DateTime now = DateTime.now();
  return DateTime(now.year, now.month, now.day + 1, 9);
}

String _defaultTitleHint(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return '例如：狂犬疫苗提醒';
    case 'deworming':
      return '例如：体内驱虫提醒';
    case 'examination':
      return '例如：年度体检提醒';
    case 'medication':
      return '例如：耳螨滴药提醒';
    case 'custom':
      return '例如：复查精神状态';
    default:
      return '请输入提醒标题';
  }
}

String _reminderTypeLabel(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return '疫苗';
    case 'deworming':
      return '驱虫';
    case 'examination':
      return '体检';
    case 'medication':
      return '用药';
    case 'custom':
      return '自定义';
    default:
      return reminderType;
  }
}

String _templateCycleLabel(ReminderTemplateSnapshot template) {
  if (template.defaultReminderMode != 'cycle') {
    return '单次提醒';
  }
  final int cycleValue = template.defaultCycleValue ?? 1;
  final String cycleUnit = template.defaultCycleUnit ?? 'month';
  return '每 $cycleValue ${_unitLabel(cycleUnit)}';
}

String _unitLabel(String unit) {
  switch (unit) {
    case 'day':
      return '天';
    case 'week':
      return '周';
    case 'month':
      return '月';
    default:
      return unit;
  }
}

String _formatDueAtLabel(DateTime dueAt) {
  final String month = dueAt.month.toString().padLeft(2, '0');
  final String day = dueAt.day.toString().padLeft(2, '0');
  final String hour = dueAt.hour.toString().padLeft(2, '0');
  final String minute = dueAt.minute.toString().padLeft(2, '0');
  return '${dueAt.year}-$month-$day $hour:$minute';
}

String? _normalizeNullableField(String value) {
  final String normalizedValue = value.trim();
  return normalizedValue.isEmpty ? null : normalizedValue;
}
