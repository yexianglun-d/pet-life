import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 首页。
class HomePage extends StatelessWidget {
  const HomePage({
    super.key,
    required this.currentUser,
    required this.dashboard,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;

  @override
  Widget build(BuildContext context) {
    final List<ReminderSnapshot> reminders =
        dashboard.reminders.take(3).toList();
    final List<HealthRecordSnapshot> healthRecords =
        dashboard.healthRecords.take(3).toList();
    final List<DailyLogSnapshot> dailyLogs =
        dashboard.dailyLogs.take(3).toList();
    final List<String> suggestions = _buildSuggestions(
      dashboard: dashboard,
      healthRecords: healthRecords,
      dailyLogs: dailyLogs,
    );

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _HeroSection(currentUser: currentUser, dashboard: dashboard),
        const SizedBox(height: 16),
        PageSection(
          title: '今天先照顾这些事',
          description: '把最需要记得的提醒放在前面，照顾起来会更安心。',
          child: _ReminderSection(reminders: reminders),
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
          const SizedBox(height: 10),
          Text(
            '${currentUser.nickname}，先看看 ${dashboard.pet.petName} 今天需要你记住什么。',
            style: textTheme.bodyMedium,
          ),
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
                      label: reminder.status == 'completed' ? '已完成' : '待处理',
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
    case 'deworming':
      return '驱虫';
    case 'physical_exam':
      return '体检';
    case 'bath':
      return '洗护';
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

Color _toReminderAccentColor(String reminderType) {
  switch (reminderType) {
    case 'physical_exam':
      return const Color(0xFFC28A52);
    case 'bath':
      return const Color(0xFF7EA9B8);
    case 'deworming':
    default:
      return const Color(0xFF7DA17A);
  }
}

IconData _toHealthRecordIcon(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return Icons.vaccines_outlined;
    case 'weight':
      return Icons.monitor_weight_outlined;
    case 'medication':
      return Icons.medication_outlined;
    default:
      return Icons.health_and_safety_outlined;
  }
}

IconData _toReminderIcon(String reminderType) {
  switch (reminderType) {
    case 'physical_exam':
      return Icons.medical_services_outlined;
    case 'bath':
      return Icons.shower_outlined;
    case 'deworming':
    default:
      return Icons.schedule_rounded;
  }
}

List<String> _buildSuggestions({
  required PetDashboardSnapshot dashboard,
  required List<HealthRecordSnapshot> healthRecords,
  required List<DailyLogSnapshot> dailyLogs,
}) {
  final List<String> suggestions = <String>[
    if (dashboard.todayTodoCount > 0)
      '先处理 ${dashboard.todayTodoCount} 条待办提醒，今天的照护会更从容。',
    if (healthRecords.isEmpty)
      '给 ${dashboard.pet.petName} 留下一条健康记录，后面回看时会更有安全感。',
    if (dailyLogs.isEmpty) '写一句今天的小日常，哪怕只是“吃饭很认真”也很值得记下来。',
  ];

  if (suggestions.isEmpty) {
    suggestions.add('今天已经照顾得很完整了，不妨再多陪 ${dashboard.pet.petName} 玩一会儿。');
    suggestions.add('如果它今天状态不错，也可以顺手记录一个开心的小瞬间。');
  }

  return suggestions.take(3).toList();
}
