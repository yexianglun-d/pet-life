/// 提醒模板快照。
class ReminderTemplateSnapshot {
  const ReminderTemplateSnapshot({
    required this.templateId,
    required this.templateName,
    required this.reminderType,
    required this.defaultReminderMode,
    required this.defaultAdvanceValue,
    required this.defaultAdvanceUnit,
    required this.applicablePetType,
    required this.enabled,
    required this.sortOrder,
    this.defaultCycleValue,
    this.defaultCycleUnit,
  });

  final String templateId;
  final String templateName;
  final String reminderType;
  final String defaultReminderMode;
  final int defaultAdvanceValue;
  final String defaultAdvanceUnit;
  final int? defaultCycleValue;
  final String? defaultCycleUnit;
  final String applicablePetType;
  final bool enabled;
  final int sortOrder;
}
