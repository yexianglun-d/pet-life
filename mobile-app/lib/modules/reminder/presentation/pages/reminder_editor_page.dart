import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';

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
  bool _isSubmitting = false;

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
    if (_isSubmitting || !_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
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
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _ReminderEditorHeroCard(reminderMode: _reminderMode),
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
                    DropdownMenuItem(value: 'observation', child: Text('观察提醒')),
                  ],
                  onChanged: (String? value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
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
                        hintText: '例如医院、药量、注意事项和执行说明',
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
    case 'observation':
      return '例如：复查精神状态';
    default:
      return '请输入提醒标题';
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
