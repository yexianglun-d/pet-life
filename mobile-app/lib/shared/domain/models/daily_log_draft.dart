/// 萌宠日常表单草稿。
class DailyLogDraft {
  const DailyLogDraft({
    required this.content,
    required this.tags,
    required this.visibility,
    required this.syncToCommunity,
    required this.happenedAt,
  });

  final String content;
  final List<String> tags;
  final String visibility;
  final bool syncToCommunity;
  final DateTime happenedAt;
}
