/// 社区举报草稿。
class CommunityReportDraft {
  const CommunityReportDraft({
    required this.reasonCode,
    this.reasonDetail,
  });

  final String reasonCode;
  final String? reasonDetail;
}
