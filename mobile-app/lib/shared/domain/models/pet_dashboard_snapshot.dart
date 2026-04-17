import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';

/// 宠物主页聚合快照。
class PetDashboardSnapshot {
  const PetDashboardSnapshot({
    required this.pet,
    required this.todayTodoCount,
    required this.reminders,
    required this.healthRecords,
    required this.dailyLogs,
  });

  final PetProfileSnapshot pet;
  final int todayTodoCount;
  final List<ReminderSnapshot> reminders;
  final List<HealthRecordSnapshot> healthRecords;
  final List<DailyLogSnapshot> dailyLogs;
}

/// 提醒快照。
class ReminderSnapshot {
  const ReminderSnapshot({
    required this.reminderId,
    required this.reminderType,
    required this.title,
    required this.dueAt,
    required this.status,
    this.notes,
  });

  final String reminderId;
  final String reminderType;
  final String title;
  final DateTime dueAt;
  final String status;
  final String? notes;
}

/// 健康记录快照。
class HealthRecordSnapshot {
  const HealthRecordSnapshot({
    required this.healthRecordId,
    required this.recordType,
    required this.title,
    required this.occurredAt,
    this.value,
    this.unit,
    this.notes,
  });

  final String healthRecordId;
  final String recordType;
  final String title;
  final DateTime occurredAt;
  final String? value;
  final String? unit;
  final String? notes;
}

/// 萌宠日常快照。
class DailyLogSnapshot {
  const DailyLogSnapshot({
    required this.dailyLogId,
    required this.content,
    required this.tags,
    required this.visibility,
    required this.happenedAt,
  });

  final String dailyLogId;
  final String content;
  final List<String> tags;
  final String visibility;
  final DateTime happenedAt;
}
