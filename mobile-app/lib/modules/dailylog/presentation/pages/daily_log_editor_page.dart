import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 萌宠日常新建/编辑页。
class DailyLogEditorPage extends StatefulWidget {
  const DailyLogEditorPage({
    super.key,
    required this.petId,
    this.initialDailyLog,
  });

  final String petId;
  final DailyLogSnapshot? initialDailyLog;

  @override
  State<DailyLogEditorPage> createState() => _DailyLogEditorPageState();
}

class _DailyLogEditorPageState extends State<DailyLogEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _contentController;
  late final TextEditingController _tagsController;
  late final TextEditingController _happenedAtController;
  late String _visibility;
  late DateTime _happenedAt;
  late bool _syncToCommunity;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    final DailyLogSnapshot? initialDailyLog = widget.initialDailyLog;
    _contentController = TextEditingController(
      text: initialDailyLog == null ? '' : initialDailyLog.content,
    );
    _tagsController = TextEditingController(
      text: initialDailyLog == null ? '' : initialDailyLog.tags.join('，'),
    );
    _visibility = initialDailyLog?.visibility ?? 'family';
    _syncToCommunity = initialDailyLog?.visibility == 'public' &&
        (initialDailyLog?.syncToCommunity ?? false);
    _happenedAt = initialDailyLog?.happenedAt ?? DateTime.now();
    _happenedAtController =
        TextEditingController(text: _formatDateTimeLabel(_happenedAt));
  }

  @override
  void dispose() {
    _contentController.dispose();
    _tagsController.dispose();
    _happenedAtController.dispose();
    super.dispose();
  }

  Future<void> _pickHappenedAt() async {
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _happenedAt,
      firstDate: DateTime(2000, 1, 1),
      lastDate: DateTime.now(),
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    final TimeOfDay? selectedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_happenedAt),
    );
    if (!mounted) {
      return;
    }

    final TimeOfDay resolvedTime =
        selectedTime ?? TimeOfDay.fromDateTime(_happenedAt);
    final DateTime selectedDateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      resolvedTime.hour,
      resolvedTime.minute,
    );

    setState(() {
      _happenedAt = selectedDateTime;
      _happenedAtController.text = _formatDateTimeLabel(selectedDateTime);
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
      final DailyLogDraft draft = DailyLogDraft(
        content: _contentController.text.trim(),
        tags: _parseTags(_tagsController.text),
        visibility: _visibility,
        syncToCommunity: _syncToCommunity,
        happenedAt: _happenedAt,
      );
      if (widget.initialDailyLog == null) {
        await repository.createDailyLog(
          petId: widget.petId,
          draft: draft,
        );
      } else {
        await repository.updateDailyLog(
          petId: widget.petId,
          dailyLogId: widget.initialDailyLog!.dailyLogId,
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
    final bool isEditMode = widget.initialDailyLog != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditMode ? '编辑萌宠日常' : '新建萌宠日常'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _DailyLogFormSection(
              title: '记录内容',
              description: '萌宠日常优先记录高价值内容，保证以后回看时能一眼知道当时发生了什么。',
              child: Column(
                children: [
                  TextFormField(
                    controller: _contentController,
                    minLines: 4,
                    maxLines: 6,
                    decoration: const InputDecoration(
                      labelText: '日常内容',
                      hintText: '例如：今天第一次主动跳上窗台晒太阳。',
                    ),
                    validator: (String? value) {
                      return value == null || value.trim().isEmpty
                          ? '请输入日常内容'
                          : null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _tagsController,
                    decoration: const InputDecoration(
                      labelText: '标签',
                      hintText: '多个标签请用中文逗号或英文逗号分隔',
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _happenedAtController,
                    readOnly: true,
                    decoration: const InputDecoration(
                      labelText: '记录时间',
                      suffixIcon: Icon(Icons.schedule_outlined),
                    ),
                    onTap: _pickHappenedAt,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _DailyLogFormSection(
              title: '可见范围',
              description: '当前阶段直接在创建时确定可见范围，避免内容沉淀后还要回头统一修正。',
              child: DropdownButtonFormField<String>(
                value: _visibility,
                decoration: const InputDecoration(labelText: '可见范围'),
                items: const [
                  DropdownMenuItem(value: 'private', child: Text('仅自己可见')),
                  DropdownMenuItem(value: 'family', child: Text('家庭可见')),
                  DropdownMenuItem(value: 'public', child: Text('公开到社区')),
                ],
                onChanged: (String? value) {
                  if (value == null) {
                    return;
                  }
                  setState(() {
                    _visibility = value;
                    if (_visibility != 'public') {
                      _syncToCommunity = false;
                    }
                  });
                },
              ),
            ),
            const SizedBox(height: 16),
            _DailyLogFormSection(
              title: '社区同步',
              description: '社区只承接公开内容。关闭同步后，这条日常仍会保留在宠物记录中，但不会出现在社区推荐流。',
              child: SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('同步到社区'),
                subtitle: Text(
                  _visibility == 'public'
                      ? '开启后会将这条公开日常同步到社区。'
                      : '只有选择“公开到社区”后才允许同步。',
                ),
                value: _syncToCommunity,
                onChanged: _visibility == 'public'
                    ? (bool value) {
                        setState(() {
                          _syncToCommunity = value;
                        });
                      }
                    : null,
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
                    : '保存日常',
          ),
        ),
      ),
    );
  }
}

class _DailyLogFormSection extends StatelessWidget {
  const _DailyLogFormSection({
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

List<String> _parseTags(String value) {
  return value
      .split(RegExp(r'[，,]'))
      .map((String item) => item.trim())
      .where((String item) => item.isNotEmpty)
      .toList();
}

String _formatDateTimeLabel(DateTime value) {
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}-$month-$day $hour:$minute';
}
