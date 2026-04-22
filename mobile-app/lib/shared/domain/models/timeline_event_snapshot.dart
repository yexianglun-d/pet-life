/// 宠物成长时间轴事件快照。
class TimelineEventSnapshot {
  const TimelineEventSnapshot({
    required this.eventId,
    required this.eventType,
    required this.sourceType,
    required this.sourceId,
    required this.eventTime,
    required this.title,
    this.summary,
    this.coverUrl,
    required this.visibility,
    this.createdAt,
  });

  final String eventId;
  final String eventType;
  final String sourceType;
  final String sourceId;
  final DateTime eventTime;
  final String title;
  final String? summary;
  final String? coverUrl;
  final String visibility;
  final DateTime? createdAt;
}
