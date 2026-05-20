import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_detail_page.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_detail_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/home_pet_report_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 宠物周期报告页。
class PetReportPage extends StatefulWidget {
  const PetReportPage({
    super.key,
    required this.reportType,
  });

  final String reportType;

  @override
  State<PetReportPage> createState() => _PetReportPageState();
}

class _PetReportPageState extends State<PetReportPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  String? _errorMessage;
  HomePetReportSnapshot? _report;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadReport();
  }

  Future<void> _loadReport() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final HomePetReportSnapshot report = widget.reportType == 'monthly'
          ? await repository.getMonthlyPetReport()
          : await repository.getWeeklyPetReport();
      if (!mounted) {
        return;
      }
      setState(() {
        _report = report;
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

  Future<void> _openHealthRecordDetail(String healthRecordId) async {
    final HomePetReportSnapshot? report = _report;
    if (report == null) {
      return;
    }
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => HealthRecordDetailPage(
          petId: report.pet.petId,
          petName: report.pet.petName,
          healthRecordId: healthRecordId,
        ),
      ),
    );
  }

  Future<void> _openDailyLogDetail(String dailyLogId) async {
    final HomePetReportSnapshot? report = _report;
    if (report == null) {
      return;
    }
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => DailyLogDetailPage(
          petId: report.pet.petId,
          petName: report.pet.petName,
          dailyLogId: dailyLogId,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.reportType == 'monthly' ? '月报' : '周报'),
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFFBF6),
              AppThemePalette.background,
            ],
          ),
        ),
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    final HomePetReportSnapshot? report = _report;
    if (_isLoading && report == null) {
      return CompanionPageLoading(
        title: widget.reportType == 'monthly' ? '正在整理月报' : '正在整理周报',
        description: '把最近的健康、提醒和日常片段先排好，报告会更容易回看。',
        icon: Icons.summarize_outlined,
        layout: CompanionLoadingLayout.detail,
      );
    }

    if (_errorMessage != null && report == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionEmptyState(
            title: '报告暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadReport,
          ),
        ],
      );
    }

    if (report == null) {
      return const SizedBox.shrink();
    }

    return RefreshIndicator(
      onRefresh: _loadReport,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _ReportHeroCard(report: report),
          const SizedBox(height: 16),
          PageSection(
            title: '这段时间的照护概览',
            description: '提醒、健康记录和陪伴片段已经整理成一眼能读懂的节奏。',
            child: _MetricGrid(
              metrics: <_MetricItem>[
                _MetricItem(
                    label: '待处理提醒', value: '${report.pendingReminderCount}'),
                _MetricItem(
                    label: '已完成提醒', value: '${report.completedReminderCount}'),
                _MetricItem(
                    label: '健康记录', value: '${report.healthRecordCount}'),
                _MetricItem(label: '萌宠日常', value: '${report.dailyLogCount}'),
                _MetricItem(
                    label: '同步社区', value: '${report.communitySyncCount}'),
                _MetricItem(
                    label: '已跳过提醒', value: '${report.skippedReminderCount}'),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '快捷记录痕迹',
            description: '喂食、饮水、排便、体重和用药的频率，会慢慢勾出日常照护节奏。',
            child: _MetricGrid(
              metrics: <_MetricItem>[
                _MetricItem(label: '喂食', value: '${report.feedCount}'),
                _MetricItem(label: '饮水', value: '${report.waterCount}'),
                _MetricItem(label: '排便', value: '${report.toiletCount}'),
                _MetricItem(label: '体重', value: '${report.weightRecordCount}'),
                _MetricItem(
                    label: '用药', value: '${report.medicationRecordCount}'),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '这段时间值得记住的点',
            description: '不是每次都需要做复杂分析，先把最重要的变化收出来就够了。',
            child: _HighlightList(highlights: report.highlights),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '最近的提醒变化',
            description: '哪些提醒处理掉了，哪些还需要继续盯着，这里会看得更清楚。',
            child: _RecentReminderList(reminders: report.recentReminders),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '最近的健康记录',
            description: '最近体检、用药和体重变化，都能从这里快速回看。',
            child: _RecentHealthRecordList(
              healthRecords: report.recentHealthRecords,
              onTap: _openHealthRecordDetail,
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '最近的陪伴片段',
            description: '除了照护事实，那些生活里的小瞬间也同样值得被看见。',
            child: _RecentDailyLogList(
              dailyLogs: report.recentDailyLogs,
              onTap: _openDailyLogDetail,
            ),
          ),
        ],
      ),
    );
  }
}

class _ReportHeroCard extends StatelessWidget {
  const _ReportHeroCard({required this.report});

  final HomePetReportSnapshot report;

  @override
  Widget build(BuildContext context) {
    final bool isMonthly = report.reportType == 'monthly';
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFE8D8),
          Color(0xFFFFFAF3),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: isMonthly ? '月度陪伴报告' : '每周陪伴报告',
            icon: isMonthly
                ? Icons.calendar_month_rounded
                : Icons.date_range_rounded,
            backgroundColor: const Color(0xFFFFE2D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(report.pet.petName,
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            '${_formatDateLabel(report.windowStart)} - ${_formatDateLabel(report.windowEnd)}',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

class _MetricGrid extends StatelessWidget {
  const _MetricGrid({required this.metrics});

  final List<_MetricItem> metrics;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: metrics
          .map(
            (_MetricItem item) => SizedBox(
              width: MediaQuery.sizeOf(context).width / 2 - 38,
              child: CompanionCard(
                padding: const EdgeInsets.all(16),
                radius: 22,
                color: AppThemePalette.surfaceRaised,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.label,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppThemePalette.muted,
                          ),
                    ),
                    const SizedBox(height: 8),
                    Text(item.value,
                        style: Theme.of(context).textTheme.titleLarge),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _HighlightList extends StatelessWidget {
  const _HighlightList({required this.highlights});

  final List<String> highlights;

  @override
  Widget build(BuildContext context) {
    if (highlights.isEmpty) {
      return const CompanionEmptyState(
        title: '这段时间还没有明显变化',
        description: '从一次快捷记录或一条新的健康记录开始，这份报告会慢慢丰富起来。',
        icon: Icons.auto_awesome_outlined,
      );
    }

    return Column(
      children: highlights
          .map(
            (String highlight) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: CompanionCard(
                padding: const EdgeInsets.all(16),
                radius: 22,
                color: AppThemePalette.surfaceRaised,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        color: AppThemePalette.warmTint,
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: const Icon(
                        Icons.favorite_border_rounded,
                        color: AppThemePalette.primaryDeep,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(child: Text(highlight)),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _RecentReminderList extends StatelessWidget {
  const _RecentReminderList({required this.reminders});

  final List<ReminderSnapshot> reminders;

  @override
  Widget build(BuildContext context) {
    if (reminders.isEmpty) {
      return const CompanionEmptyState(
        title: '这段时间没有提醒变化',
        description: '新的提醒建立后，这里会把待处理和已处理的节奏整理出来。',
        icon: Icons.schedule_rounded,
      );
    }

    return Column(
      children: reminders
          .map(
            (ReminderSnapshot reminder) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: CompanionCard(
                padding: const EdgeInsets.all(16),
                radius: 22,
                color: AppThemePalette.surfaceRaised,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        CompanionPill(
                            label: _toLocalizedReminderType(
                                reminder.reminderType)),
                        CompanionPill(
                          label: _toLocalizedReminderStatus(reminder.status),
                          backgroundColor: AppThemePalette.warmTint,
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(reminder.title,
                        style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 6),
                    Text(
                      _formatDateTimeLabel(reminder.dueAt),
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppThemePalette.muted,
                          ),
                    ),
                    if (reminder.notes != null &&
                        reminder.notes!.trim().isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(reminder.notes!),
                      ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _RecentHealthRecordList extends StatelessWidget {
  const _RecentHealthRecordList({
    required this.healthRecords,
    required this.onTap,
  });

  final List<HealthRecordSnapshot> healthRecords;
  final ValueChanged<String> onTap;

  @override
  Widget build(BuildContext context) {
    if (healthRecords.isEmpty) {
      return const CompanionEmptyState(
        title: '这段时间没有新的健康记录',
        description: '下次体检、体重或用药记录完成后，这里会自动补上。',
        icon: Icons.health_and_safety_outlined,
      );
    }

    return Column(
      children: healthRecords
          .map(
            (HealthRecordSnapshot record) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: InkWell(
                onTap: () => onTap(record.healthRecordId),
                borderRadius: BorderRadius.circular(22),
                child: Ink(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppThemePalette.surfaceRaised,
                    borderRadius: BorderRadius.circular(22),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(record.title,
                                style: Theme.of(context).textTheme.titleMedium),
                            const SizedBox(height: 6),
                            Text(
                              _buildHealthRecordSubtitle(record),
                              style: Theme.of(context)
                                  .textTheme
                                  .bodyMedium
                                  ?.copyWith(
                                    color: AppThemePalette.muted,
                                  ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Icon(
                        Icons.chevron_right,
                        color: AppThemePalette.muted,
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

class _RecentDailyLogList extends StatelessWidget {
  const _RecentDailyLogList({
    required this.dailyLogs,
    required this.onTap,
  });

  final List<DailyLogSnapshot> dailyLogs;
  final ValueChanged<String> onTap;

  @override
  Widget build(BuildContext context) {
    if (dailyLogs.isEmpty) {
      return const CompanionEmptyState(
        title: '这段时间还没有新的陪伴片段',
        description: '下一次玩耍、散步或者安静的小瞬间，也会成为这里的一部分。',
        icon: Icons.auto_awesome_outlined,
      );
    }

    return Column(
      children: dailyLogs
          .map(
            (DailyLogSnapshot dailyLog) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: InkWell(
                onTap: () => onTap(dailyLog.dailyLogId),
                borderRadius: BorderRadius.circular(22),
                child: Ink(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppThemePalette.surfaceRaised,
                    borderRadius: BorderRadius.circular(22),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(dailyLog.content,
                          style: Theme.of(context).textTheme.bodyLarge),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: dailyLog.tags
                            .map((String tag) => CompanionPill(label: '#$tag'))
                            .toList(),
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

class _MetricItem {
  const _MetricItem({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;
}

String _formatDateLabel(DateTime value) {
  return '${value.month} 月 ${value.day} 日';
}

String _formatDateTimeLabel(DateTime value) {
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.month} 月 ${value.day} 日 $hour:$minute';
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

String _toLocalizedReminderStatus(String status) {
  switch (status) {
    case 'completed':
      return '已完成';
    case 'skipped':
      return '已跳过';
    default:
      return '待处理';
  }
}

String _buildHealthRecordSubtitle(HealthRecordSnapshot record) {
  final List<String> parts = <String>[
    _formatDateTimeLabel(record.occurredAt),
    if (record.value != null && record.unit != null)
      '${record.value} ${record.unit}',
    if (record.notes != null && record.notes!.trim().isNotEmpty) record.notes!,
  ];
  return parts.join(' · ');
}
