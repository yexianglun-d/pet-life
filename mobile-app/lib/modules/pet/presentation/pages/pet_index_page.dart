import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_list_page.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_list_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_management_page.dart';
import 'package:petlife_mobile_app/modules/reminder/presentation/pages/reminder_list_page.dart';
import 'package:petlife_mobile_app/modules/timeline/presentation/pages/timeline_page.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 宠物主页索引页。
///
/// 当前阶段先把宠物主页需要承接的摘要能力稳定下来，后续接入接口时优先替换数据装配，
/// 不打乱页面结构和用户认知路径。
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
          title: '宠物概览',
          description: '主页先承接宠物主档、统计指标和关键动作，后续再扩成完整档案页。',
          child: _MetricSection(metrics: metrics),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '健康档案',
          description: '疫苗、体重、用药和体检等结构化记录都会在这里聚合回看。',
          child: _HealthArchiveSection(
            healthRecords: dashboard.healthRecords.take(3).toList(),
            onOpenHealthRecords: () => _openHealthRecords(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '提醒计划',
          description: '提醒完成后会自动影响首页待办和宠物主页摘要。',
          child: _ReminderPlanSection(
            reminders: dashboard.reminders.take(3).toList(),
            onOpenReminders: () => _openReminders(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '萌宠日常',
          description: '日常记录先沉淀为宠物资产，再决定是否同步到社区。',
          child: _DailyEntrySection(
            dailyLogs: dashboard.dailyLogs.take(3).toList(),
            onOpenDailyLogs: () => _openDailyLogs(context),
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '成长时间轴',
          description: '把健康记录和萌宠日常串在一条线上回看，方便判断宠物近期发生了哪些关键变化。',
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

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Row(
        children: [
          Container(
            width: 68,
            height: 68,
            decoration: BoxDecoration(
              color: const Color(0xFFDCFCE7),
              borderRadius: BorderRadius.circular(22),
            ),
            child: const Icon(Icons.pets_outlined,
                size: 34, color: Color(0xFF166534)),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(dashboard.pet.petName, style: textTheme.titleLarge),
                const SizedBox(height: 6),
                Text(
                  '${dashboard.pet.breed} · ${_toLocalizedGender(dashboard.pet.gender)} · ${_toLocalizedPetType(dashboard.pet.petType)}',
                  style: textTheme.bodyMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  '当前由 ${currentUser.familyName} 共享照护，主页优先聚合提醒、健康和萌宠日常摘要。',
                  style: textTheme.bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: onManagePetPressed,
                  child: const Text('管理宠物'),
                ),
              ],
            ),
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
        ? const _EmptySectionPlaceholder(label: '还没有健康档案记录')
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
                    ?.copyWith(color: const Color(0xFF64748B)),
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
        ? const _EmptySectionPlaceholder(label: '当前没有提醒计划')
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
                    ?.copyWith(color: const Color(0xFF64748B)),
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
        ? const _EmptySectionPlaceholder(label: '还没有萌宠日常记录')
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
                    ?.copyWith(color: const Color(0xFF64748B)),
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
    return Row(
      children: [
        Expanded(
          child: Text(
            '当前已接入健康记录与萌宠日常事件，后续会继续补服务记录和设备事件。',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
        ),
        const SizedBox(width: 12),
        FilledButton(
          onPressed: onOpenTimeline,
          child: const Text('查看时间轴'),
        ),
      ],
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.metric});

  final _PetMetric metric;

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
          Text(metric.label, style: textTheme.bodyMedium),
          const SizedBox(height: 8),
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

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 12,
          height: 12,
          margin: const EdgeInsets.only(top: 6),
          decoration: BoxDecoration(
            color: leadingColor,
            shape: BoxShape.circle,
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
                style: textTheme.bodyMedium
                    ?.copyWith(color: const Color(0xFF64748B)),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ReminderPlanCard extends StatelessWidget {
  const _ReminderPlanCard({required this.entry});

  final ReminderSnapshot entry;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
      ),
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
                  style: textTheme.bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: const Color(0xFFDCFCE7),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              entry.status == 'completed' ? '已完成' : '待处理',
              style: textTheme.bodyMedium
                  ?.copyWith(color: const Color(0xFF166534)),
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

class _EmptySectionPlaceholder extends StatelessWidget {
  const _EmptySectionPlaceholder({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      style: Theme.of(context)
          .textTheme
          .bodyMedium
          ?.copyWith(color: const Color(0xFF64748B)),
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
