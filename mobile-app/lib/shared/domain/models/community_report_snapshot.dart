/// 社区举报结果快照。
class CommunityReportSnapshot {
  const CommunityReportSnapshot({
    required this.reportId,
    required this.targetType,
    required this.targetId,
    required this.reasonCode,
    required this.status,
    required this.createdAt,
    this.reasonDetail,
  });

  final String reportId;
  final String targetType;
  final String targetId;
  final String reasonCode;
  final String? reasonDetail;
  final String status;
  final DateTime createdAt;
}
