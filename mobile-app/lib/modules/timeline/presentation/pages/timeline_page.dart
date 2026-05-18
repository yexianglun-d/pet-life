import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_detail_page.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_detail_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/timeline_event_snapshot.dart';

/// 成长时间轴页。
class TimelinePage extends StatefulWidget {
  const TimelinePage({
    super.key,
    required this.petId,
    required this.petName,
  });

  final String petId;
  final String petName;

  @override
  State<TimelinePage> createState() => _TimelinePageState();
}

class _TimelinePageState extends State<TimelinePage> {
  static const List<_TimelineFilter> _filters = <_TimelineFilter>[
    _TimelineFilter(key: 'all', label: '全部'),
    _TimelineFilter(key: 'health', label: '健康'),
    _TimelineFilter(key: 'daily_log', label: '日常'),
  ];

  bool _didLoad = false;
  bool _isLoading = false;
  bool _hasChanges = false;
  String _selectedFilter = 'all';
  String? _errorMessage;
  List<TimelineEventSnapshot> _timelineEvents = const <TimelineEventSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }

    _didLoad = true;
    _loadTimelineEvents();
  }

  Future<void> _loadTimelineEvents() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<TimelineEventSnapshot> timelineEvents =
          await repository.listTimelineEvents(
        petId: widget.petId,
        eventType: _selectedFilter,
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _timelineEvents = timelineEvents;
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

  Future<void> _changeFilter(String filterKey) async {
    if (_selectedFilter == filterKey) {
      return;
    }

    setState(() {
      _selectedFilter = filterKey;
    });
    await _loadTimelineEvents();
  }

  Future<void> _openEventDetail(TimelineEventSnapshot event) async {
    bool? changed;
    if (event.sourceType == 'health_record') {
      changed = await Navigator.of(context).push<bool>(
        MaterialPageRoute<bool>(
          builder: (_) => HealthRecordDetailPage(
            petId: widget.petId,
            petName: widget.petName,
            healthRecordId: event.sourceId,
          ),
        ),
      );
    } else if (event.sourceType == 'daily_log') {
      changed = await Navigator.of(context).push<bool>(
        MaterialPageRoute<bool>(
          builder: (_) => DailyLogDetailPage(
            petId: widget.petId,
            petName: widget.petName,
            dailyLogId: event.sourceId,
          ),
        ),
      );
    } else {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('当前事件暂不支持详情跳转')),
      );
      return;
    }

    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadTimelineEvents();
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
          title: const Text('成长时间轴'),
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
              _TimelineHeroCard(
                petName: widget.petName,
                filterKey: _selectedFilter,
                eventCount: _timelineEvents.length,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '想看哪一类记录',
                description: '把健康和日常放在同一条成长轨迹里，再按需要轻轻筛一下。',
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _filters
                      .map(
                        (_TimelineFilter filter) => ChoiceChip(
                          label: Text(filter.label),
                          selected: _selectedFilter == filter.key,
                          onSelected: (_) => _changeFilter(filter.key),
                        ),
                      )
                      .toList(),
                ),
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '成长片段',
                description: '每一条记录都像陪伴里的一个节点，点进去就能回到当时的详细内容。',
                child: _buildTimelineList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTimelineList() {
    if (_isLoading && _timelineEvents.isEmpty) {
      return const CompanionSkeletonList(
        itemCount: 4,
      );
    }

    if (_errorMessage != null && _timelineEvents.isEmpty) {
      return CompanionEmptyState(
        title: '时间轴暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadTimelineEvents,
      );
    }

    if (_timelineEvents.isEmpty) {
      return const CompanionEmptyState(
        title: '这一类时间轴还没有内容',
        description: '等健康记录或萌宠日常慢慢积累起来，这里就会连成一条完整的成长轨迹。',
        icon: Icons.timeline_rounded,
      );
    }

    return Column(
      children: _timelineEvents
          .map(
            (TimelineEventSnapshot event) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _TimelineEventCard(
                event: event,
                onTap: () => _openEventDetail(event),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _TimelineHeroCard extends StatelessWidget {
  const _TimelineHeroCard({
    required this.petName,
    required this.filterKey,
    required this.eventCount,
  });

  final String petName;
  final String filterKey;
  final int eventCount;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDC),
          Color(0xFFFFFAF3),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '成长时间轴',
            icon: Icons.timeline_rounded,
            backgroundColor: Color(0xFFFFE3D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(petName, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            '把体检、疫苗、驱虫和日常小片段放在一起回看，会更容易看见陪伴的痕迹。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '当前筛选 ${_toFilterLabel(filterKey)}',
                backgroundColor: AppThemePalette.surface,
              ),
              CompanionPill(
                label: '当前 $eventCount 条',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _TimelineEventCard extends StatelessWidget {
  const _TimelineEventCard({
    required this.event,
    required this.onTap,
  });

  final TimelineEventSnapshot event;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final _TimelineCardStyle style = _toTimelineCardStyle(event.eventType);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppThemePalette.surfaceRaised,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: AppThemePalette.line),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: style.backgroundColor,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Icon(style.icon, color: style.foregroundColor),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          event.title,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                      ),
                      const SizedBox(width: 10),
                      CompanionPill(
                        label: style.label,
                        backgroundColor: style.backgroundColor,
                        foregroundColor: style.foregroundColor,
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _buildEventDescription(event),
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: AppThemePalette.muted,
                        ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TimelineFilter {
  const _TimelineFilter({
    required this.key,
    required this.label,
  });

  final String key;
  final String label;
}

class _TimelineCardStyle {
  const _TimelineCardStyle({
    required this.label,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
  });

  final String label;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
}

String _toFilterLabel(String filterKey) {
  switch (filterKey) {
    case 'health':
      return '健康';
    case 'daily_log':
      return '日常';
    default:
      return '全部';
  }
}

_TimelineCardStyle _toTimelineCardStyle(String eventType) {
  switch (eventType) {
    case 'health':
      return const _TimelineCardStyle(
        label: '健康',
        icon: Icons.favorite_outline,
        backgroundColor: Color(0xFFE8F3E7),
        foregroundColor: AppThemePalette.success,
      );
    case 'daily_log':
      return const _TimelineCardStyle(
        label: '日常',
        icon: Icons.auto_stories_outlined,
        backgroundColor: Color(0xFFFFE9D6),
        foregroundColor: AppThemePalette.primaryDeep,
      );
    default:
      return const _TimelineCardStyle(
        label: '事件',
        icon: Icons.schedule_outlined,
        backgroundColor: Color(0xFFF3E9DF),
        foregroundColor: AppThemePalette.muted,
      );
  }
}

String _buildEventDescription(TimelineEventSnapshot event) {
  final DateTime eventTime = event.eventTime;
  final String month = eventTime.month.toString().padLeft(2, '0');
  final String day = eventTime.day.toString().padLeft(2, '0');
  final String hour = eventTime.hour.toString().padLeft(2, '0');
  final String minute = eventTime.minute.toString().padLeft(2, '0');
  final List<String> parts = <String>[
    '${eventTime.year}-$month-$day $hour:$minute',
    if (event.summary != null && event.summary!.trim().isNotEmpty)
      event.summary!,
  ];
  return parts.join(' · ');
}
