/// 健康记录表单草稿。
class HealthRecordDraft {
  const HealthRecordDraft({
    required this.recordType,
    required this.title,
    required this.occurredAt,
    this.value,
    this.unit,
    this.hospitalName,
    this.doctorName,
    this.severityLevel,
    this.resultSummary,
    this.attachmentAssetIds = const <String>[],
    this.nextReminderAt,
    this.nextReminderTitle,
    this.notes,
  });

  final String recordType;
  final String title;
  final DateTime occurredAt;
  final String? value;
  final String? unit;
  final String? hospitalName;
  final String? doctorName;
  final String? severityLevel;
  final String? resultSummary;
  final List<String> attachmentAssetIds;
  final DateTime? nextReminderAt;
  final String? nextReminderTitle;
  final String? notes;
}
