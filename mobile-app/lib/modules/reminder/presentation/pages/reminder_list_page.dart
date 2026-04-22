import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/reminder/presentation/pages/reminder_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 提醒列表页。
class ReminderListPage extends StatefulWidget {
  const ReminderListPage({
    super.key,
    required this.petId,
    required this.petName,
  });

  final String petId;
  final String petName;

  @override
  State<ReminderListPage> createState() => _ReminderListPageState();
}

class _ReminderListPageState extends State<ReminderListPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _hasChanges = false;
  String? _errorMessage;
  String? _submittingReminderId;
  List<ReminderSnapshot> _reminders = const <ReminderSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }

    _didLoad = true;
    _loadReminders();
  }

  Future<void> _loadReminders() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<ReminderSnapshot> reminders =
          await repository.listReminders(widget.petId);
      if (!mounted) {
        return;
      }

      setState(() {
        _reminders = reminders;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        _errorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _openCreateReminderPage() async {
    final bool? created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ReminderEditorPage(petId: widget.petId),
      ),
    );
    if (!mounted || created != true) {
      return;
    }

    _hasChanges = true;
    await _loadReminders();
  }

  Future<void> _completeReminder(String reminderId) async {
    if (_submittingReminderId != null) {
      return;
    }

    setState(() {
      _submittingReminderId = reminderId;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.completeReminder(
        petId: widget.petId,
        reminderId: reminderId,
      );
      _hasChanges = true;
      await _loadReminders();
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
          _submittingReminderId = null;
        });
      }
    }
  }

  Future<void> _skipReminder(String reminderId) async {
    if (_submittingReminderId != null) {
      return;
    }

    setState(() {
      _submittingReminderId = reminderId;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.skipReminder(
        petId: widget.petId,
        reminderId: reminderId,
      );
      _hasChanges = true;
      await _loadReminders();
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
          _submittingReminderId = null;
        });
      }
    }
  }

  Future<bool> _handleWillPop() async {
    Navigator.of(context).pop(_hasChanges);
    return false;
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: _handleWillPop,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('提醒计划'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            PageSection(
              title: widget.petName,
              description: '提醒计划优先服务日常照护闭环，完成动作会直接影响首页待办和宠物页摘要。',
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '当前已配置 ${_reminders.length} 条提醒',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                  FilledButton(
                    onPressed: _openCreateReminderPage,
                    child: const Text('新增提醒'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '提醒列表',
              description: '当前已支持新建、完成和跳过，周期能力后续继续展开。',
              child: _buildReminderList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReminderList() {
    if (_isLoading && _reminders.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _reminders.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _errorMessage!,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFFB91C1C)),
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: _loadReminders,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (_reminders.isEmpty) {
      return const Text('当前还没有提醒计划，先新增一条吧。');
    }

    return Column(
      children: _reminders
          .map(
            (ReminderSnapshot reminder) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _ReminderCard(
                reminder: reminder,
                isSubmitting: _submittingReminderId == reminder.reminderId,
                onComplete: reminder.status == 'pending'
                    ? () => _completeReminder(reminder.reminderId)
                    : null,
                onSkip: reminder.status == 'pending'
                    ? () => _skipReminder(reminder.reminderId)
                    : null,
              ),
            ),
          )
          .toList(),
    );
  }
}

class _ReminderCard extends StatelessWidget {
  const _ReminderCard({
    required this.reminder,
    required this.isSubmitting,
    this.onComplete,
    this.onSkip,
  });

  final ReminderSnapshot reminder;
  final bool isSubmitting;
  final VoidCallback? onComplete;
  final VoidCallback? onSkip;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(reminder.title, style: textTheme.titleMedium),
                    const SizedBox(height: 6),
                    Text(
                      _buildReminderDescription(reminder),
                      style: textTheme.bodyMedium
                          ?.copyWith(color: const Color(0xFF64748B)),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              _ReminderStatusTag(status: reminder.status),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: const Color(0xFFE2E8F0),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  _toLocalizedReminderType(reminder.reminderType),
                  style: textTheme.bodyMedium,
                ),
              ),
              const Spacer(),
              if (onSkip != null) ...[
                OutlinedButton(
                  onPressed: isSubmitting ? null : onSkip,
                  child: Text(isSubmitting ? '处理中...' : '跳过'),
                ),
                const SizedBox(width: 8),
              ],
              if (onComplete != null)
                FilledButton.tonal(
                  onPressed: isSubmitting ? null : onComplete,
                  child: Text(isSubmitting ? '处理中...' : '完成提醒'),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ReminderStatusTag extends StatelessWidget {
  const _ReminderStatusTag({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final bool isCompleted = status == 'completed';
    final bool isSkipped = status == 'skipped';
    final Color backgroundColor = isCompleted
        ? const Color(0xFFDCFCE7)
        : isSkipped
            ? const Color(0xFFE0F2FE)
            : const Color(0xFFFEF3C7);
    final Color foregroundColor = isCompleted
        ? const Color(0xFF166534)
        : isSkipped
            ? const Color(0xFF0F766E)
            : const Color(0xFF92400E);
    final String label = isCompleted
        ? '已完成'
        : isSkipped
            ? '已跳过'
            : '待处理';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: foregroundColor,
            ),
      ),
    );
  }
}

String _buildReminderDescription(ReminderSnapshot reminder) {
  final String month = reminder.dueAt.month.toString().padLeft(2, '0');
  final String day = reminder.dueAt.day.toString().padLeft(2, '0');
  final String hour = reminder.dueAt.hour.toString().padLeft(2, '0');
  final String minute = reminder.dueAt.minute.toString().padLeft(2, '0');

  final List<String> parts = <String>[
    '${reminder.dueAt.year}-$month-$day $hour:$minute',
    if (reminder.reminderMode == 'cycle') _buildReminderCycleLabel(reminder),
    if (reminder.notes != null && reminder.notes!.trim().isNotEmpty)
      reminder.notes!,
  ];
  return parts.join(' · ');
}

String _buildReminderCycleLabel(ReminderSnapshot reminder) {
  final int? cycleValue = reminder.cycleValue;
  final String? cycleUnit = reminder.cycleUnit;
  if (cycleValue == null || cycleUnit == null) {
    return '周期提醒';
  }

  return '每$cycleValue${_toLocalizedCycleUnit(cycleUnit)}';
}

String _toLocalizedReminderType(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return '疫苗';
    case 'deworming':
      return '驱虫';
    case 'examination':
      return '体检';
    case 'medication':
      return '用药';
    case 'observation':
      return '观察提醒';
    default:
      return reminderType;
  }
}

String _toLocalizedCycleUnit(String cycleUnit) {
  switch (cycleUnit) {
    case 'day':
      return '天';
    case 'week':
      return '周';
    case 'month':
      return '月';
    default:
      return cycleUnit;
  }
}
