import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_editor_page.dart';
import 'package:petlife_mobile_app/modules/home/presentation/pages/home_reminder_center_page.dart';
import 'package:petlife_mobile_app/modules/home/presentation/pages/pet_report_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/health_record_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 首页。
class HomePage extends StatefulWidget {
  const HomePage({
    super.key,
    required this.currentUser,
    required this.dashboard,
    required this.onHomeDataChanged,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;
  final VoidCallback onHomeDataChanged;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String? _submittingQuickRecordKey;

  Future<void> _openReminderCenter() async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HomeReminderCenterPage(
          petId: widget.dashboard.pet.petId,
          petName: widget.dashboard.pet.petName,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }
    widget.onHomeDataChanged();
  }

  Future<void> _openReportPage(String reportType) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => PetReportPage(reportType: reportType),
      ),
    );
  }

  Future<void> _openDailyLogEditor() async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => DailyLogEditorPage(petId: widget.dashboard.pet.petId),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }
    widget.onHomeDataChanged();
  }

  Future<void> _handleQuickRecordAction(_QuickRecordAction action) async {
    if (_submittingQuickRecordKey != null) {
      return;
    }

    switch (action.key) {
      case 'daily_log':
        await _openDailyLogEditor();
        return;
      case 'feed':
      case 'water':
      case 'toilet':
        await _submitSimpleDailyQuickRecord(action);
        return;
      case 'weight':
        await _submitWeightQuickRecord();
        return;
      case 'medication':
        await _submitMedicationQuickRecord();
        return;
    }
  }

  Future<void> _submitSimpleDailyQuickRecord(_QuickRecordAction action) async {
    final String? note = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        return _SimpleQuickRecordSheet(action: action);
      },
    );
    if (!mounted || note == null) {
      return;
    }

    await _runQuickRecord(
      action.key,
      successMessage: '${action.label}已经记下来了',
      task: () async {
        final repository = PetLifeAppScope.repositoryOf(context);
        await repository.createDailyLog(
          petId: widget.dashboard.pet.petId,
          draft: DailyLogDraft(
            content: _buildQuickRecordContent(action, note),
            tags: <String>['快捷记录', action.label],
            visibility: 'family',
            syncToCommunity: false,
            happenedAt: DateTime.now(),
          ),
        );
      },
    );
  }

  Future<void> _submitWeightQuickRecord() async {
    final _WeightQuickRecordSubmission? submission =
        await showModalBottomSheet<_WeightQuickRecordSubmission>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        return const _WeightQuickRecordSheet();
      },
    );
    if (!mounted || submission == null) {
      return;
    }

    await _runQuickRecord(
      'weight',
      successMessage: '体重记录已经存进健康档案',
      task: () async {
        final repository = PetLifeAppScope.repositoryOf(context);
        await repository.createHealthRecord(
          petId: widget.dashboard.pet.petId,
          draft: HealthRecordDraft(
            recordType: 'weight',
            title: '体重记录',
            occurredAt: DateTime.now(),
            value: submission.weightValue,
            unit: 'kg',
            notes: submission.notes,
          ),
        );
      },
    );
  }

  Future<void> _submitMedicationQuickRecord() async {
    final _MedicationQuickRecordSubmission? submission =
        await showModalBottomSheet<_MedicationQuickRecordSubmission>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        return const _MedicationQuickRecordSheet();
      },
    );
    if (!mounted || submission == null) {
      return;
    }

    await _runQuickRecord(
      'medication',
      successMessage: '用药记录已经存进健康档案',
      task: () async {
        final repository = PetLifeAppScope.repositoryOf(context);
        await repository.createHealthRecord(
          petId: widget.dashboard.pet.petId,
          draft: HealthRecordDraft(
            recordType: 'medication',
            title: submission.title,
            occurredAt: DateTime.now(),
            notes: submission.notes,
          ),
        );
      },
    );
  }

  Future<void> _runQuickRecord(
    String actionKey, {
    required String successMessage,
    required Future<void> Function() task,
  }) async {
    setState(() {
      _submittingQuickRecordKey = actionKey;
    });

    try {
      await task();
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(successMessage)),
      );
      widget.onHomeDataChanged();
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
          _submittingQuickRecordKey = null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final List<ReminderSnapshot> reminders =
        widget.dashboard.reminders.take(3).toList();
    final List<HealthRecordSnapshot> healthRecords =
        widget.dashboard.healthRecords.take(3).toList();
    final List<DailyLogSnapshot> dailyLogs =
        widget.dashboard.dailyLogs.take(3).toList();
    final List<String> suggestions = _buildSuggestions(
      dashboard: widget.dashboard,
      healthRecords: healthRecords,
      dailyLogs: dailyLogs,
    );

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _HeroSection(
            currentUser: widget.currentUser, dashboard: widget.dashboard),
        const SizedBox(height: 16),
        PageSection(
          title: '顺手记一条',
          description: '喂食、饮水、排便、体重和用药，都可以直接从首页记下，不用再绕进去找页面。',
          child: _QuickRecordSection(
            actions: _quickRecordActions,
            submittingActionKey: _submittingQuickRecordKey,
            onActionTap: _handleQuickRecordAction,
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '今天先照顾这些事',
          description: '把最需要记得的提醒放在前面，照顾起来会更安心。',
          actionLabel: '查看全部',
          onAction: _openReminderCenter,
          child: _ReminderSection(reminders: reminders),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '这段时间的陪伴回看',
          description: '周报和月报会把最近的照护节奏、健康变化和生活片段整理成一份更清楚的回看。',
          child: _ReportEntrySection(onOpenReport: _openReportPage),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '最近的健康变化',
          description: '最近发生过什么，回头看时一眼就能想起来。',
          child: _HealthRecordSection(healthRecords: healthRecords),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '陪伴小片段',
          description: '平常的小瞬间也值得被认真记住。',
          child: _DailyLogSection(dailyLogs: dailyLogs),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '今天可以顺手做的事',
          description: '不知道先做什么时，就从最轻松的一件开始。',
          child: _SuggestionSection(suggestions: suggestions),
        ),
      ],
    );
  }
}

class _HeroSection extends StatelessWidget {
  const _HeroSection({
    required this.currentUser,
    required this.dashboard,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEBDD),
          Color(0xFFFFF8F1),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '今天也在认真陪伴它',
            icon: Icons.favorite_rounded,
            backgroundColor: Color(0xFFFFE2D1),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 8),
          Text('宠物生活管家', style: textTheme.headlineSmall),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: _HeroMetricCard(
                  label: '当前宠物',
                  value: dashboard.pet.petName,
                  hint:
                      '${dashboard.pet.breed} · ${_toLocalizedPetType(dashboard.pet.petType)}',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _HeroMetricCard(
                  label: '今天待办',
                  value: dashboard.todayTodoCount.toString(),
                  hint: '${currentUser.familyName} 一起照顾',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuickRecordSection extends StatelessWidget {
  const _QuickRecordSection({
    required this.actions,
    required this.submittingActionKey,
    required this.onActionTap,
  });

  final List<_QuickRecordAction> actions;
  final String? submittingActionKey;
  final ValueChanged<_QuickRecordAction> onActionTap;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: actions
          .map(
            (_QuickRecordAction action) => SizedBox(
              width: MediaQuery.sizeOf(context).width / 3 - 24,
              child: InkWell(
                onTap: submittingActionKey == null
                    ? () => onActionTap(action)
                    : null,
                borderRadius: BorderRadius.circular(22),
                child: Ink(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppThemePalette.surfaceRaised,
                    borderRadius: BorderRadius.circular(22),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 42,
                        height: 42,
                        decoration: BoxDecoration(
                          color: action.accentColor.withValues(alpha: 0.18),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: submittingActionKey == action.key
                            ? Icon(
                                Icons.hourglass_top_rounded,
                                color: action.accentColor,
                              )
                            : Icon(action.icon, color: action.accentColor),
                      ),
                      const SizedBox(height: 12),
                      Text(action.label,
                          style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 6),
                      Text(
                        action.hint,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppThemePalette.muted,
                            ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _ReminderSection extends StatelessWidget {
  const _ReminderSection({required this.reminders});

  final List<ReminderSnapshot> reminders;

  @override
  Widget build(BuildContext context) {
    if (reminders.isEmpty) {
      return const CompanionEmptyState(
        title: '今天没有待处理提醒',
        description: '可以安心陪它玩一会儿，或者顺手记下一条新的小日常。',
        icon: Icons.spa_outlined,
      );
    }

    return Column(
      children: reminders
          .map(
            (ReminderSnapshot reminder) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _ReminderItemCard(reminder: reminder),
            ),
          )
          .toList(),
    );
  }
}

class _ReportEntrySection extends StatelessWidget {
  const _ReportEntrySection({required this.onOpenReport});

  final ValueChanged<String> onOpenReport;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _ReportEntryCard(
          title: '看看这周过得怎么样',
          description: '把最近 7 天的提醒、健康记录和陪伴片段整理成一份周报。',
          icon: Icons.date_range_rounded,
          onTap: () => onOpenReport('weekly'),
        ),
        const SizedBox(height: 12),
        _ReportEntryCard(
          title: '再回头看看这个月',
          description: '把最近 30 天的节奏和变化收成一份月报，适合做更完整的回看。',
          icon: Icons.calendar_month_rounded,
          onTap: () => onOpenReport('monthly'),
        ),
      ],
    );
  }
}

class _ReportEntryCard extends StatelessWidget {
  const _ReportEntryCard({
    required this.title,
    required this.description,
    required this.icon,
    required this.onTap,
  });

  final String title;
  final String description;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppThemePalette.surfaceRaised,
          borderRadius: BorderRadius.circular(22),
        ),
        child: Row(
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(
                color: AppThemePalette.warmTint,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(icon, color: AppThemePalette.primaryDeep),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleMedium),
                ],
              ),
            ),
            const SizedBox(width: 12),
            const Icon(Icons.chevron_right, color: AppThemePalette.muted),
          ],
        ),
      ),
    );
  }
}

class _HealthRecordSection extends StatelessWidget {
  const _HealthRecordSection({required this.healthRecords});

  final List<HealthRecordSnapshot> healthRecords;

  @override
  Widget build(BuildContext context) {
    if (healthRecords.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有健康记录',
        description: '第一次记录也很重要，之后回看会更容易发现变化。',
        icon: Icons.health_and_safety_outlined,
      );
    }

    return Column(
      children: healthRecords
          .map(
            (HealthRecordSnapshot record) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _HealthDigestCard(record: record),
            ),
          )
          .toList(),
    );
  }
}

class _DailyLogSection extends StatelessWidget {
  const _DailyLogSection({required this.dailyLogs});

  final List<DailyLogSnapshot> dailyLogs;

  @override
  Widget build(BuildContext context) {
    if (dailyLogs.isEmpty) {
      return const CompanionEmptyState(
        title: '今天还没有记录日常',
        description: '一句话、一张照片，都会是以后回头看时很温柔的记忆。',
        icon: Icons.auto_awesome_outlined,
      );
    }

    return Column(
      children: dailyLogs
          .map(
            (DailyLogSnapshot dailyLog) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _DailyMomentCard(dailyLog: dailyLog),
            ),
          )
          .toList(),
    );
  }
}

class _SuggestionSection extends StatelessWidget {
  const _SuggestionSection({required this.suggestions});

  final List<String> suggestions;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: suggestions
          .map(
            (String suggestion) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: CompanionCard(
                color: AppThemePalette.surfaceRaised,
                radius: 22,
                padding: const EdgeInsets.all(16),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 38,
                      height: 38,
                      decoration: BoxDecoration(
                        color: AppThemePalette.warmTint,
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: const Icon(
                        Icons.wb_sunny_outlined,
                        color: AppThemePalette.primaryDeep,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(child: Text(suggestion)),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _HeroMetricCard extends StatelessWidget {
  const _HeroMetricCard({
    required this.label,
    required this.value,
    required this.hint,
  });

  final String label;
  final String value;
  final String hint;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0x73FFFFFF),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: textTheme.bodyMedium),
          const SizedBox(height: 8),
          Text(value, style: textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(
            hint,
            style: textTheme.bodySmall?.copyWith(
              color: AppThemePalette.body,
            ),
          ),
        ],
      ),
    );
  }
}

class _ReminderItemCard extends StatelessWidget {
  const _ReminderItemCard({required this.reminder});

  final ReminderSnapshot reminder;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: _toReminderAccentColor(reminder.reminderType).withValues(
                alpha: 0.18,
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Icon(
              _toReminderIcon(reminder.reminderType),
              color: _toReminderAccentColor(reminder.reminderType),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    CompanionPill(
                      label: _toLocalizedReminderType(reminder.reminderType),
                      backgroundColor: _toReminderAccentColor(
                        reminder.reminderType,
                      ).withValues(alpha: 0.16),
                      foregroundColor: _toReminderAccentColor(
                        reminder.reminderType,
                      ),
                    ),
                    CompanionPill(
                      label: reminder.status == 'completed'
                          ? '已完成'
                          : reminder.status == 'skipped'
                              ? '已跳过'
                              : '待处理',
                      backgroundColor: AppThemePalette.warmTint,
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Text(reminder.title, style: textTheme.titleMedium),
                const SizedBox(height: 6),
                Text(
                  _formatDueAt(reminder.dueAt),
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
                ),
                if (reminder.notes != null && reminder.notes!.trim().isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      reminder.notes!,
                      style: textTheme.bodySmall?.copyWith(
                        color: AppThemePalette.body,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HealthDigestCard extends StatelessWidget {
  const _HealthDigestCard({required this.record});

  final HealthRecordSnapshot record;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: AppThemePalette.sky.withValues(alpha: 0.22),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(
              _toHealthRecordIcon(record.recordType),
              color: const Color(0xFF517C8A),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(record.title, style: textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  _buildHealthRecordSubtitle(record),
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DailyMomentCard extends StatelessWidget {
  const _DailyMomentCard({required this.dailyLog});

  final DailyLogSnapshot dailyLog;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(dailyLog.content, style: textTheme.bodyMedium),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              CompanionPill(
                label: _toLocalizedVisibility(dailyLog.visibility),
                backgroundColor: AppThemePalette.sky.withValues(alpha: 0.2),
                foregroundColor: AppThemePalette.title,
              ),
              if (dailyLog.syncToCommunity)
                const CompanionPill(
                  label: '已同步到社区',
                  icon: Icons.forum_outlined,
                  backgroundColor: Color(0xFFE9F3E7),
                  foregroundColor: Color(0xFF65846D),
                ),
              ...dailyLog.tags
                  .map((String tag) => CompanionPill(label: '#$tag')),
            ],
          ),
        ],
      ),
    );
  }
}

class _SimpleQuickRecordSheet extends StatefulWidget {
  const _SimpleQuickRecordSheet({required this.action});

  final _QuickRecordAction action;

  @override
  State<_SimpleQuickRecordSheet> createState() =>
      _SimpleQuickRecordSheetState();
}

class _SimpleQuickRecordSheetState extends State<_SimpleQuickRecordSheet> {
  final TextEditingController _noteController = TextEditingController();

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _QuickRecordSheetScaffold(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          _QuickRecordSheetHeader(
            title: '记录一次${widget.action.label}',
            description: '会按当前时间写进萌宠日常，后面回看时能更清楚地看到照护节奏。',
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _noteController,
            minLines: 2,
            maxLines: 4,
            decoration: InputDecoration(
              labelText: '补充一句备注',
              hintText: widget.action.placeholder,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '将按现在的时间记录，并默认设为家庭可见。',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('取消'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: () => Navigator.of(context).pop(
                    _noteController.text.trim(),
                  ),
                  child: const Text('保存记录'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _WeightQuickRecordSheet extends StatefulWidget {
  const _WeightQuickRecordSheet();

  @override
  State<_WeightQuickRecordSheet> createState() =>
      _WeightQuickRecordSheetState();
}

class _WeightQuickRecordSheetState extends State<_WeightQuickRecordSheet> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _weightController = TextEditingController();
  final TextEditingController _notesController = TextEditingController();

  @override
  void dispose() {
    _weightController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _QuickRecordSheetScaffold(
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const _QuickRecordSheetHeader(
              title: '记录一次体重',
              description: '体重会直接存进健康档案，后面在周报和月报里也能一起回看。',
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _weightController,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: '体重（kg）',
                hintText: '例如 4.6',
              ),
              validator: (String? value) {
                final String text = value?.trim() ?? '';
                if (text.isEmpty) {
                  return '请输入体重';
                }
                return double.tryParse(text) == null ? '请输入有效数字' : null;
              },
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _notesController,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(
                labelText: '备注',
                hintText: '例如称重前刚吃完饭，或者今天状态很好',
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('取消'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: () {
                      if (!_formKey.currentState!.validate()) {
                        return;
                      }
                      Navigator.of(context).pop(
                        _WeightQuickRecordSubmission(
                          weightValue: _weightController.text.trim(),
                          notes: _normalizeNullableText(_notesController.text),
                        ),
                      );
                    },
                    child: const Text('保存记录'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MedicationQuickRecordSheet extends StatefulWidget {
  const _MedicationQuickRecordSheet();

  @override
  State<_MedicationQuickRecordSheet> createState() =>
      _MedicationQuickRecordSheetState();
}

class _MedicationQuickRecordSheetState
    extends State<_MedicationQuickRecordSheet> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _notesController = TextEditingController();

  @override
  void dispose() {
    _titleController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _QuickRecordSheetScaffold(
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const _QuickRecordSheetHeader(
              title: '记录一次用药',
              description: '这条记录会直接进入健康档案，后面回看时就不会只记得“好像喂过药”。',
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: '用药标题',
                hintText: '例如 耳螨滴药 / 体外驱虫 / 益生菌',
              ),
              validator: (String? value) {
                return value == null || value.trim().isEmpty ? '请输入用药标题' : null;
              },
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _notesController,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(
                labelText: '备注',
                hintText: '例如用量、饭前饭后或执行时的状态',
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('取消'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: () {
                      if (!_formKey.currentState!.validate()) {
                        return;
                      }
                      Navigator.of(context).pop(
                        _MedicationQuickRecordSubmission(
                          title: _titleController.text.trim(),
                          notes: _normalizeNullableText(_notesController.text),
                        ),
                      );
                    },
                    child: const Text('保存记录'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickRecordSheetScaffold extends StatelessWidget {
  const _QuickRecordSheetScaffold({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final EdgeInsets viewInsets = MediaQuery.viewInsetsOf(context);
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 0, 16, viewInsets.bottom + 16),
      child: CompanionCard(
        padding: const EdgeInsets.all(20),
        radius: 28,
        color: AppThemePalette.surface,
        child: child,
      ),
    );
  }
}

class _QuickRecordSheetHeader extends StatelessWidget {
  const _QuickRecordSheetHeader({
    required this.title,
    required this.description,
  });

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const CompanionPill(
          label: '快捷记录',
          icon: Icons.bolt_rounded,
          backgroundColor: Color(0xFFFFE3D2),
          foregroundColor: AppThemePalette.primaryDeep,
        ),
        const SizedBox(height: 12),
        Text(title, style: Theme.of(context).textTheme.titleLarge),
      ],
    );
  }
}

class _QuickRecordAction {
  const _QuickRecordAction({
    required this.key,
    required this.label,
    required this.hint,
    required this.placeholder,
    required this.icon,
    required this.accentColor,
  });

  final String key;
  final String label;
  final String hint;
  final String placeholder;
  final IconData icon;
  final Color accentColor;
}

class _WeightQuickRecordSubmission {
  const _WeightQuickRecordSubmission({
    required this.weightValue,
    this.notes,
  });

  final String weightValue;
  final String? notes;
}

class _MedicationQuickRecordSubmission {
  const _MedicationQuickRecordSubmission({
    required this.title,
    this.notes,
  });

  final String title;
  final String? notes;
}

const List<_QuickRecordAction> _quickRecordActions = <_QuickRecordAction>[
  _QuickRecordAction(
    key: 'feed',
    label: '喂食',
    hint: '记下今天这顿',
    placeholder: '例如胃口不错，吃得很干净',
    icon: Icons.restaurant_rounded,
    accentColor: Color(0xFFC67D4A),
  ),
  _QuickRecordAction(
    key: 'water',
    label: '饮水',
    hint: '补一条喝水记录',
    placeholder: '例如今天喝水明显比平时多一点',
    icon: Icons.water_drop_rounded,
    accentColor: Color(0xFF5D92A5),
  ),
  _QuickRecordAction(
    key: 'toilet',
    label: '排便',
    hint: '记下今天状态',
    placeholder: '例如状态正常，没有异常味道或颜色',
    icon: Icons.sanitizer_rounded,
    accentColor: Color(0xFF7A8E55),
  ),
  _QuickRecordAction(
    key: 'weight',
    label: '体重',
    hint: '直接存进健康档案',
    placeholder: '记录体重变化',
    icon: Icons.monitor_weight_rounded,
    accentColor: Color(0xFF876EA0),
  ),
  _QuickRecordAction(
    key: 'medication',
    label: '用药',
    hint: '补一条用药事实',
    placeholder: '记录药物和备注',
    icon: Icons.medication_rounded,
    accentColor: Color(0xFFAF6B6B),
  ),
  _QuickRecordAction(
    key: 'daily_log',
    label: '记日常',
    hint: '写一句陪伴片段',
    placeholder: '打开日常编辑页',
    icon: Icons.edit_note_rounded,
    accentColor: Color(0xFF9A7A52),
  ),
];

List<String> _buildSuggestions({
  required PetDashboardSnapshot dashboard,
  required List<HealthRecordSnapshot> healthRecords,
  required List<DailyLogSnapshot> dailyLogs,
}) {
  final List<String> suggestions = <String>[];
  if (dashboard.todayTodoCount > 0) {
    suggestions.add('今天还有 ${dashboard.todayTodoCount} 条待办提醒，可以先从最近的一条开始处理。');
  }
  if (healthRecords.isEmpty) {
    suggestions.add('可以先补一条最近的健康记录，之后回看时会更容易发现变化。');
  }
  if (dailyLogs.isEmpty) {
    suggestions.add('今天还没有记录陪伴片段，哪怕只写一句小观察也很值得。');
  }
  if (suggestions.isEmpty) {
    suggestions.add('今天的照护节奏不错，不妨顺手看看这周的陪伴报告。');
  }
  return suggestions;
}

String _buildQuickRecordContent(_QuickRecordAction action, String note) {
  final String baseContent;
  switch (action.key) {
    case 'feed':
      baseContent = '快捷记录：今天完成了一次喂食。';
      break;
    case 'water':
      baseContent = '快捷记录：今天补记了一次饮水。';
      break;
    case 'toilet':
      baseContent = '快捷记录：今天补记了一次排便观察。';
      break;
    default:
      baseContent = '快捷记录：补记了一条新的照护事实。';
      break;
  }
  final String trimmedNote = note.trim();
  if (trimmedNote.isEmpty) {
    return baseContent;
  }
  return '$baseContent\n备注：$trimmedNote';
}

String? _normalizeNullableText(String value) {
  final String normalizedValue = value.trim();
  return normalizedValue.isEmpty ? null : normalizedValue;
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

String _toLocalizedReminderType(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return '疫苗';
    case 'deworming':
      return '驱虫';
    case 'examination':
    case 'physical_exam':
      return '体检';
    case 'medication':
      return '用药';
    default:
      return reminderType;
  }
}

String _formatDueAt(DateTime dueAt) {
  final String hour = dueAt.hour.toString().padLeft(2, '0');
  final String minute = dueAt.minute.toString().padLeft(2, '0');
  return '${dueAt.month} 月 ${dueAt.day} 日 $hour:$minute';
}

String _buildHealthRecordSubtitle(HealthRecordSnapshot record) {
  final List<String> parts = <String>[
    if (record.value != null && record.unit != null)
      '${record.value} ${record.unit}',
    '${record.occurredAt.month}-${record.occurredAt.day}',
    if (record.notes != null && record.notes!.trim().isNotEmpty) record.notes!,
  ];
  return parts.join(' · ');
}

String _toLocalizedVisibility(String visibility) {
  switch (visibility) {
    case 'public':
      return '公开到社区';
    case 'family':
      return '家庭可见';
    case 'private':
      return '仅自己可见';
    default:
      return visibility;
  }
}

IconData _toReminderIcon(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return Icons.vaccines_rounded;
    case 'deworming':
      return Icons.pest_control_rounded;
    case 'examination':
    case 'physical_exam':
      return Icons.medical_services_rounded;
    case 'medication':
      return Icons.medication_rounded;
    default:
      return Icons.notifications_active_rounded;
  }
}

Color _toReminderAccentColor(String reminderType) {
  switch (reminderType) {
    case 'vaccine':
      return const Color(0xFF7A8E55);
    case 'deworming':
      return const Color(0xFFB07355);
    case 'examination':
    case 'physical_exam':
      return const Color(0xFF6C8A96);
    case 'medication':
      return const Color(0xFFB06B68);
    default:
      return AppThemePalette.primaryDeep;
  }
}

IconData _toHealthRecordIcon(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return Icons.vaccines_rounded;
    case 'deworming':
      return Icons.pest_control_rounded;
    case 'examination':
      return Icons.monitor_heart_rounded;
    case 'medication':
      return Icons.medication_rounded;
    case 'weight':
      return Icons.monitor_weight_rounded;
    default:
      return Icons.favorite_outline_rounded;
  }
}
