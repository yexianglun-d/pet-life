import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_list_page.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_list_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_management_page.dart';
import 'package:petlife_mobile_app/modules/reminder/presentation/pages/reminder_list_page.dart';
import 'package:petlife_mobile_app/modules/timeline/presentation/pages/timeline_page.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 宠物主页索引页。
class PetIndexPage extends StatelessWidget {
  const PetIndexPage({
    super.key,
    required this.currentUser,
    required this.dashboard,
    required this.onPetDataChanged,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;
  final VoidCallback onPetDataChanged;

  Future<void> _openPetManagement(BuildContext context) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => PetManagementPage(
          initialCurrentPetId: currentUser.currentPetId,
        ),
      ),
    );
    if (!context.mounted || changed != true) {
      return;
    }

    onPetDataChanged();
  }

  Future<void> _openHealthRecords(BuildContext context) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HealthRecordListPage(
          petId: dashboard.pet.petId,
          petName: dashboard.pet.petName,
        ),
      ),
    );
    if (!context.mounted || changed != true) {
      return;
    }

    onPetDataChanged();
  }

  Future<void> _openReminders(BuildContext context) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ReminderListPage(
          petId: dashboard.pet.petId,
          petName: dashboard.pet.petName,
        ),
      ),
    );
    if (!context.mounted || changed != true) {
      return;
    }

    onPetDataChanged();
  }

  Future<void> _openDailyLogs(BuildContext context) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => DailyLogListPage(
          petId: dashboard.pet.petId,
          petName: dashboard.pet.petName,
        ),
      ),
    );
    if (!context.mounted || changed != true) {
      return;
    }

    onPetDataChanged();
  }

  Future<void> _openTimeline(BuildContext context) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => TimelinePage(
          petId: dashboard.pet.petId,
          petName: dashboard.pet.petName,
        ),
      ),
    );
    if (!context.mounted || changed != true) {
      return;
    }

    onPetDataChanged();
  }

  @override
  Widget build(BuildContext context) {
    final List<_PetMetric> metrics = <_PetMetric>[
      _PetMetric(label: '待办提醒', value: dashboard.todayTodoCount.toString()),
      _PetMetric(
          label: '健康记录', value: dashboard.healthRecords.length.toString()),
      _PetMetric(label: '萌宠日常', value: dashboard.dailyLogs.length.toString()),
    ];

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _PetHeroCard(
          currentUser: currentUser,
          dashboard: dashboard,
          onManagePetPressed: () => _openPetManagement(context),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '最近的照护概览',
          description: '把最常回看的状态先放在眼前，照顾起来会更顺手。',
          child: _MetricSection(metrics: metrics),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '健康档案',
          description: '体检、疫苗、用药和异常观察，都会慢慢沉淀成完整档案。',
          child: _HealthArchiveSection(
            healthRecords: dashboard.healthRecords.take(3).toList(),
            onOpenHealthRecords: () => _openHealthRecords(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '提醒计划',
          description: '把需要记住的时间点排好，平时就不会总担心漏掉。',
          child: _ReminderPlanSection(
            reminders: dashboard.reminders.take(3).toList(),
            onOpenReminders: () => _openReminders(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '萌宠日常',
          description: '生活里的小片段，会把它慢慢拼成更真实的样子。',
          child: _DailyEntrySection(
            dailyLogs: dashboard.dailyLogs.take(3).toList(),
            onOpenDailyLogs: () => _openDailyLogs(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '成长时间轴',
          description: '把重要变化串成一条线，回头看时会更安心也更清晰。',
          child: _TimelineEntrySection(
            onOpenTimeline: () => _openTimeline(context),
          ),
        ),
      ],
    );
  }
}

class _PetHeroCard extends StatelessWidget {
  const _PetHeroCard({
    required this.currentUser,
    required this.dashboard,
    required this.onManagePetPressed,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;
  final VoidCallback onManagePetPressed;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFE9DC),
          Color(0xFFFFFBF7),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '正在照顾的小可爱',
            icon: Icons.pets_rounded,
            backgroundColor: Color(0xFFFFE1D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 14),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Icon(
                  dashboard.pet.petType == 'dog'
                      ? Icons.pets_rounded
                      : Icons.cruelty_free_outlined,
                  size: 34,
                  color: AppThemePalette.primaryDeep,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(dashboard.pet.petName, style: textTheme.headlineSmall),
                    const SizedBox(height: 8),
                    Text(
                      '${dashboard.pet.breed} · ${_toLocalizedGender(dashboard.pet.gender)} · ${_toLocalizedPetType(dashboard.pet.petType)}',
                      style: textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${currentUser.familyName} 正一起照顾它，这里会把成长、健康和日常慢慢整理成一份完整档案。',
                      style: textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.body,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '待办 ${dashboard.todayTodoCount}',
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '健康 ${dashboard.healthRecords.length}',
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '日常 ${dashboard.dailyLogs.length}',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
          const SizedBox(height: 18),
          OutlinedButton(
            onPressed: onManagePetPressed,
            child: const Text('管理宠物'),
          ),
        ],
      ),
    );
  }
}

class _MetricSection extends StatelessWidget {
  const _MetricSection({required this.metrics});

  final List<_PetMetric> metrics;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: metrics
          .map(
            (_PetMetric metric) => Expanded(
              child: Padding(
                padding: EdgeInsets.only(
                  right: metric == metrics.last ? 0 : 12,
                ),
                child: _MetricCard(metric: metric),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _HealthArchiveSection extends StatelessWidget {
  const _HealthArchiveSection({
    required this.healthRecords,
    required this.onOpenHealthRecords,
  });

  final List<HealthRecordSnapshot> healthRecords;
  final VoidCallback onOpenHealthRecords;

  @override
  Widget build(BuildContext context) {
    final Widget content = healthRecords.isEmpty
        ? const CompanionEmptyState(
            title: '还没有健康档案记录',
            description: '第一次体检、疫苗或用药记录，都会成为以后回看的起点。',
            icon: Icons.health_and_safety_outlined,
          )
        : Column(
            children: healthRecords
                .map(
                  (HealthRecordSnapshot record) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _TimelineCard(
                      title: record.title,
                      description: _buildHealthDescription(record),
                      leadingColor: const Color(0xFF0F766E),
                    ),
                  ),
                )
                .toList(),
          );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                '已收录 ${healthRecords.length} 条记录',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: AppThemePalette.muted),
              ),
            ),
            TextButton(
              onPressed: onOpenHealthRecords,
              child: const Text('查看全部'),
            ),
          ],
        ),
        const SizedBox(height: 8),
        content,
      ],
    );
  }
}

class _ReminderPlanSection extends StatelessWidget {
  const _ReminderPlanSection({
    required this.reminders,
    required this.onOpenReminders,
  });

  final List<ReminderSnapshot> reminders;
  final VoidCallback onOpenReminders;

  @override
  Widget build(BuildContext context) {
    final Widget content = reminders.isEmpty
        ? const CompanionEmptyState(
            title: '还没有提醒计划',
            description: '把驱虫、体检或自定义照护时间排好，会轻松很多。',
            icon: Icons.schedule_rounded,
          )
        : Column(
            children: reminders
                .map(
                  (ReminderSnapshot entry) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _ReminderPlanCard(entry: entry),
                  ),
                )
                .toList(),
          );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                '已配置 ${reminders.length} 条提醒',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: AppThemePalette.muted),
              ),
            ),
            TextButton(
              onPressed: onOpenReminders,
              child: const Text('查看全部'),
            ),
          ],
        ),
        const SizedBox(height: 8),
        content,
      ],
    );
  }
}

class _DailyEntrySection extends StatelessWidget {
  const _DailyEntrySection({
    required this.dailyLogs,
    required this.onOpenDailyLogs,
  });

  final List<DailyLogSnapshot> dailyLogs;
  final VoidCallback onOpenDailyLogs;

  @override
  Widget build(BuildContext context) {
    final Widget content = dailyLogs.isEmpty
        ? const CompanionEmptyState(
            title: '还没有萌宠日常记录',
            description: '它今天的样子、情绪和可爱时刻，都可以从这里开始留下。',
            icon: Icons.auto_awesome_outlined,
          )
        : Column(
            children: dailyLogs
                .map(
                  (DailyLogSnapshot entry) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _TimelineCard(
                      title: entry.content,
                      description:
                          '${_toLocalizedVisibility(entry.visibility)} · ${entry.tags.join(' / ')}',
                      leadingColor: const Color(0xFFB45309),
                    ),
                  ),
                )
                .toList(),
          );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                '已记录 ${dailyLogs.length} 条日常',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: AppThemePalette.muted),
              ),
            ),
            TextButton(
              onPressed: onOpenDailyLogs,
              child: const Text('查看全部'),
            ),
          ],
        ),
        const SizedBox(height: 8),
        content,
      ],
    );
  }
}

class _TimelineEntrySection extends StatelessWidget {
  const _TimelineEntrySection({
    required this.onOpenTimeline,
  });

  final VoidCallback onOpenTimeline;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      padding: const EdgeInsets.all(18),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '把每一次体检、提醒和日常小事放到同一条线里，回看会很有安全感。',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.body,
                  ),
            ),
          ),
          const SizedBox(width: 12),
          FilledButton(
            onPressed: onOpenTimeline,
            child: const Text('查看时间轴'),
          ),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.metric});

  final _PetMetric metric;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 22,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(metric.label, style: textTheme.bodyMedium),
          const SizedBox(height: 10),
          Text(metric.value, style: textTheme.titleLarge),
        ],
      ),
    );
  }
}

class _TimelineCard extends StatelessWidget {
  const _TimelineCard({
    required this.title,
    required this.description,
    required this.leadingColor,
  });

  final String title;
  final String description;
  final Color leadingColor;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 22,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: leadingColor.withValues(alpha: 0.16),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(
              Icons.favorite_border_rounded,
              color: leadingColor,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  description,
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

class _ReminderPlanCard extends StatelessWidget {
  const _ReminderPlanCard({required this.entry});

  final ReminderSnapshot entry;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 22,
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(entry.title, style: textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  _formatDueAt(entry.dueAt),
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: entry.status == 'completed'
                  ? AppThemePalette.mint.withValues(alpha: 0.2)
                  : AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              entry.status == 'completed' ? '已完成' : '待处理',
              style: textTheme.bodySmall?.copyWith(
                color: entry.status == 'completed'
                    ? const Color(0xFF65846D)
                    : AppThemePalette.primaryDeep,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PetMetric {
  const _PetMetric({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;
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

String _toLocalizedGender(String? gender) {
  switch (gender) {
    case 'female':
      return '母';
    case 'male':
      return '公';
    default:
      return gender ?? '未知';
  }
}

String _formatDueAt(DateTime dueAt) {
  final String hour = dueAt.hour.toString().padLeft(2, '0');
  final String minute = dueAt.minute.toString().padLeft(2, '0');
  return '${dueAt.month} 月 ${dueAt.day} 日 $hour:$minute';
}

String _buildHealthDescription(HealthRecordSnapshot record) {
  final List<String> parts = <String>[
    if (record.value != null && record.unit != null)
      '${record.value} ${record.unit}',
    '${record.occurredAt.year}-${record.occurredAt.month.toString().padLeft(2, '0')}-${record.occurredAt.day.toString().padLeft(2, '0')}',
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
