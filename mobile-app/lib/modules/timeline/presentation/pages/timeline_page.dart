import 'package:flutter/material.dart';
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
          title: const Text('成长时间轴'),
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
              description: '时间轴把健康记录和萌宠日常串成同一条生命周期视图，便于回看关键节点。',
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
              title: '事件列表',
              description: '当前已接入健康记录与萌宠日常，后续会继续补服务、设备等事件来源。',
              child: _buildTimelineList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimelineList() {
    if (_isLoading && _timelineEvents.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _timelineEvents.isEmpty) {
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
            onPressed: _loadTimelineEvents,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (_timelineEvents.isEmpty) {
      return const Text('当前筛选条件下还没有时间轴事件。');
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
      borderRadius: BorderRadius.circular(18),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: style.backgroundColor,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(style.icon, color: style.foregroundColor),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          event.title,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: style.backgroundColor,
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          style.label,
                          style:
                              Theme.of(context).textTheme.bodyMedium?.copyWith(
                                    color: style.foregroundColor,
                                  ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _buildEventDescription(event),
                    style: Theme.of(context)
                        .textTheme
                        .bodyMedium
                        ?.copyWith(color: const Color(0xFF64748B)),
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

_TimelineCardStyle _toTimelineCardStyle(String eventType) {
  switch (eventType) {
    case 'health':
      return const _TimelineCardStyle(
        label: '健康',
        icon: Icons.favorite_outline,
        backgroundColor: Color(0xFFDCFCE7),
        foregroundColor: Color(0xFF166534),
      );
    case 'daily_log':
      return const _TimelineCardStyle(
        label: '日常',
        icon: Icons.auto_stories_outlined,
        backgroundColor: Color(0xFFFDE68A),
        foregroundColor: Color(0xFF92400E),
      );
    default:
      return const _TimelineCardStyle(
        label: '事件',
        icon: Icons.schedule_outlined,
        backgroundColor: Color(0xFFE2E8F0),
        foregroundColor: Color(0xFF334155),
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
