import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
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
    this.pageTitle = '提醒计划',
    this.heroLabel = '提醒计划',
    this.heroDescription = '把接下来要记住的时间点都排好，就不会总担心忘记。',
    this.sectionTitle = '接下来要记住的事',
    this.sectionDescription = '驱虫、体检、用药和复查提醒都会整理在这里，不容易漏掉。',
    this.emptyTitle = '还没有提醒计划',
    this.emptyDescription = '把重要的时间点先排好，之后照顾起来会更从容。',
    this.createButtonLabel = '新增提醒',
  });

  final String petId;
  final String petName;
  final String pageTitle;
  final String heroLabel;
  final String heroDescription;
  final String sectionTitle;
  final String sectionDescription;
  final String emptyTitle;
  final String emptyDescription;
  final String createButtonLabel;

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

  @override
  Widget build(BuildContext context) {
    return PopScope<bool>(
      canPop: false,
      onPopInvokedWithResult: (bool didPop, bool? result) {
        if (didPop) {
          return;
        }
        Navigator.of(context).pop(_hasChanges);
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text(widget.pageTitle),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
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
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _ReminderHeroSection(
                petName: widget.petName,
                reminderCount: _reminders.length,
                onCreate: _openCreateReminderPage,
                heroLabel: widget.heroLabel,
                heroDescription: widget.heroDescription,
                createButtonLabel: widget.createButtonLabel,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: widget.sectionTitle,
                description: widget.sectionDescription,
                child: _buildReminderList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildReminderList() {
    if (_isLoading && _reminders.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (_errorMessage != null && _reminders.isEmpty) {
      return CompanionEmptyState(
        title: '提醒计划暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadReminders,
      );
    }

    if (_reminders.isEmpty) {
      return CompanionEmptyState(
        title: widget.emptyTitle,
        description: widget.emptyDescription,
        icon: Icons.schedule_rounded,
      );
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

class _ReminderHeroSection extends StatelessWidget {
  const _ReminderHeroSection({
    required this.petName,
    required this.reminderCount,
    required this.onCreate,
    required this.heroLabel,
    required this.heroDescription,
    required this.createButtonLabel,
  });

  final String petName;
  final int reminderCount;
  final VoidCallback onCreate;
  final String heroLabel;
  final String heroDescription;
  final String createButtonLabel;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEADB),
          Color(0xFFFFF9F4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: heroLabel,
            icon: Icons.alarm_rounded,
            backgroundColor: Color(0xFFFFE0CE),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(petName, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            heroDescription,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          CompanionPill(
            label: '当前共 $reminderCount 条',
            backgroundColor: AppThemePalette.surface,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onCreate,
            child: Text(createButtonLabel),
          ),
        ],
      ),
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

    return CompanionCard(
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
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
                      style: textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.muted,
                      ),
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
              CompanionPill(
                label: _toLocalizedReminderType(reminder.reminderType),
                backgroundColor: AppThemePalette.surface,
              ),
              const Spacer(),
              if (onSkip != null)
                OutlinedButton(
                  onPressed: isSubmitting ? null : onSkip,
                  child: const Text('跳过'),
                ),
              if (onComplete != null) ...[
                const SizedBox(width: 8),
                FilledButton.tonal(
                  onPressed: isSubmitting ? null : onComplete,
                  child: Text(isSubmitting ? '处理中...' : '完成'),
                ),
              ],
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
    switch (status) {
      case 'completed':
        return const CompanionPill(
          label: '已完成',
          backgroundColor: Color(0xFFE8F3E7),
          foregroundColor: Color(0xFF65846D),
        );
      case 'skipped':
        return const CompanionPill(
          label: '已跳过',
          backgroundColor: Color(0xFFF8E3DF),
          foregroundColor: Color(0xFFB96C62),
        );
      default:
        return const CompanionPill(
          label: '待处理',
          backgroundColor: Color(0xFFE4EEF1),
          foregroundColor: Color(0xFF61808A),
        );
    }
  }
}

String _buildReminderDescription(ReminderSnapshot reminder) {
  final String dueAtLabel = _formatDueAtLabel(reminder.dueAt);
  final List<String> parts = <String>[
    dueAtLabel,
    reminder.reminderMode == 'cycle'
        ? '每 ${reminder.cycleValue}${_toLocalizedCycleUnit(reminder.cycleUnit)}'
        : '单次提醒',
    if (reminder.notes != null && reminder.notes!.trim().isNotEmpty)
      reminder.notes!,
  ];
  return parts.join(' · ');
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

String _toLocalizedCycleUnit(String? cycleUnit) {
  switch (cycleUnit) {
    case 'day':
      return '天';
    case 'week':
      return '周';
    case 'month':
      return '个月';
    default:
      return cycleUnit ?? '';
  }
}

String _formatDueAtLabel(DateTime dueAt) {
  final String month = dueAt.month.toString().padLeft(2, '0');
  final String day = dueAt.day.toString().padLeft(2, '0');
  final String hour = dueAt.hour.toString().padLeft(2, '0');
  final String minute = dueAt.minute.toString().padLeft(2, '0');
  return '${dueAt.year}-$month-$day $hour:$minute';
}
