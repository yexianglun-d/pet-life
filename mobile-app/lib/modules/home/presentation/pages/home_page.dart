import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 首页驾驶舱。
///
/// 当前阶段先以稳定的静态聚合结构承接首页信息层级，后续接入真实接口时只替换数据来源，
/// 不重做页面骨架和组件边界。
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

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _HeroSection(currentUser: currentUser, dashboard: dashboard),
        const SizedBox(height: 16),
        PageSection(
          title: '今日待办',
          description: '首页先聚合驱虫、体检和自定义提醒，后续再接通知中心与日历视图。',
          child: _ReminderSection(reminders: reminders),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '近期健康记录',
          description: '最近完成的健康事件会回写到宠物档案和成长时间轴。',
          child: _HealthRecordSection(healthRecords: healthRecords),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '萌宠日常',
          description: '记录会先沉淀在宠物维度，后续再按可见范围流向社区。',
          child: _DailyLogSection(dailyLogs: dailyLogs),
        ),
        const SizedBox(height: 16),
        const PageSection(
          title: '当前实现边界',
          description: '本期先打通主链路，商城和设备维持真实占位，不提前接半成品后端。',
          child: _ScopeSection(),
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

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('宠物生活管家', style: textTheme.headlineSmall),
          const SizedBox(height: 8),
          Text(
            '${currentUser.nickname}，今天先处理 ${dashboard.pet.petName} 的提醒和健康记录。',
            style: textTheme.bodyMedium,
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: _QuickMetricCard(
                  label: '当前宠物',
                  value: dashboard.pet.petName,
                  hint:
                      '${dashboard.pet.breed} · ${_toLocalizedPetType(dashboard.pet.petType)}',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _QuickMetricCard(
                  label: '待办',
                  value: dashboard.todayTodoCount.toString(),
                  hint: '${currentUser.familyName} 共享照护',
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
      return const _EmptySectionPlaceholder(label: '今天没有待处理提醒');
    }

    return Column(
      children: reminders
          .map(
            (ReminderSnapshot reminder) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _ReminderCard(reminder: reminder),
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
      return const _EmptySectionPlaceholder(label: '还没有健康记录');
    }

    return Column(
      children: healthRecords
          .map(
            (HealthRecordSnapshot record) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _HealthRecordCard(record: record),
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
      return const _EmptySectionPlaceholder(label: '今天还没有记录萌宠日常');
    }

    return Column(
      children: dailyLogs
          .map(
            (DailyLogSnapshot dailyLog) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _DailyLogCard(dailyLog: dailyLog),
            ),
          )
          .toList(),
    );
  }
}

class _ScopeSection extends StatelessWidget {
  const _ScopeSection();

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: const [
        _ScopeChip(label: '宠物主档'),
        _ScopeChip(label: '健康记录'),
        _ScopeChip(label: '提醒'),
        _ScopeChip(label: '萌宠日常'),
        _ScopeChip(label: '社区预留'),
        _ScopeChip(label: '服务预约预留'),
      ],
    );
  }
}

class _QuickMetricCard extends StatelessWidget {
  const _QuickMetricCard({
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
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(18),
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
            style:
                textTheme.bodyMedium?.copyWith(color: const Color(0xFF64748B)),
          ),
        ],
      ),
    );
  }
}

class _ReminderCard extends StatelessWidget {
  const _ReminderCard({required this.reminder});

  final ReminderSnapshot reminder;

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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 10,
            height: 10,
            margin: const EdgeInsets.only(top: 6),
            decoration: BoxDecoration(
              color: _toReminderAccentColor(reminder.reminderType),
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_toLocalizedReminderType(reminder.reminderType),
                    style: textTheme.bodyMedium),
                const SizedBox(height: 4),
                Text(reminder.title, style: textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  _formatDueAt(reminder.dueAt),
                  style: textTheme.bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HealthRecordCard extends StatelessWidget {
  const _HealthRecordCard({required this.record});

  final HealthRecordSnapshot record;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Row(
      children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: const Color(0xFFECFEFF),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Icon(_toHealthRecordIcon(record.recordType),
              color: const Color(0xFF155E75)),
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

class _DailyLogCard extends StatelessWidget {
  const _DailyLogCard({required this.dailyLog});

  final DailyLogSnapshot dailyLog;

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
          Text(dailyLog.content, style: textTheme.bodyMedium),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ...dailyLog.tags.map((String tag) => _TagChip(label: tag)),
              _TagChip(
                label: _toLocalizedVisibility(dailyLog.visibility),
                backgroundColor: const Color(0xFFDCFCE7),
                foregroundColor: const Color(0xFF166534),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ScopeChip extends StatelessWidget {
  const _ScopeChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(label),
    );
  }
}

class _TagChip extends StatelessWidget {
  const _TagChip({
    required this.label,
    this.backgroundColor = const Color(0xFFE2E8F0),
    this.foregroundColor = const Color(0xFF334155),
  });

  final String label;
  final Color backgroundColor;
  final Color foregroundColor;

  @override
  Widget build(BuildContext context) {
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
      return const Color(0xFFB45309);
    case 'bath':
      return const Color(0xFF2563EB);
    case 'deworming':
    default:
      return const Color(0xFF0F766E);
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
