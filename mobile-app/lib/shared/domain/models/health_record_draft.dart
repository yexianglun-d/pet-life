/// 健康记录表单草稿。
class HealthRecordDraft {
  const HealthRecordDraft({
    required this.recordType,
    required this.title,
    required this.occurredAt,
    this.value,
    this.unit,
    this.notes,
  });

  final String recordType;
  final String title;
  final DateTime occurredAt;
  final String? value;
  final String? unit;
  final String? notes;
}
