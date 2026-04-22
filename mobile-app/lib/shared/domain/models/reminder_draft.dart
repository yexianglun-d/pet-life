/// 提醒表单草稿。
class ReminderDraft {
  const ReminderDraft({
    required this.reminderType,
    required this.title,
    required this.dueAt,
    this.reminderMode = 'single',
    this.cycleValue,
    this.cycleUnit,
    this.notes,
  });

  final String reminderType;
  final String title;
  final DateTime dueAt;
  final String reminderMode;
  final int? cycleValue;
  final String? cycleUnit;
  final String? notes;
}
