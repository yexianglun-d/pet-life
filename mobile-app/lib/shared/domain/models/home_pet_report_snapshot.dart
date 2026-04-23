import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物周期报告快照。
///
/// 首页周报和月报都复用同一份读模型，避免不同页面各自维护统计字段与最近事件结构。
class HomePetReportSnapshot {
  const HomePetReportSnapshot({
    required this.reportType,
    required this.pet,
    required this.windowStart,
    required this.windowEnd,
    required this.pendingReminderCount,
    required this.completedReminderCount,
    required this.skippedReminderCount,
    required this.healthRecordCount,
    required this.dailyLogCount,
    required this.communitySyncCount,
    required this.feedCount,
    required this.waterCount,
    required this.toiletCount,
    required this.weightRecordCount,
    required this.medicationRecordCount,
    required this.highlights,
    required this.recentReminders,
    required this.recentHealthRecords,
    required this.recentDailyLogs,
  });

  final String reportType;
  final PetDetailSnapshot pet;
  final DateTime windowStart;
  final DateTime windowEnd;
  final int pendingReminderCount;
  final int completedReminderCount;
  final int skippedReminderCount;
  final int healthRecordCount;
  final int dailyLogCount;
  final int communitySyncCount;
  final int feedCount;
  final int waterCount;
  final int toiletCount;
  final int weightRecordCount;
  final int medicationRecordCount;
  final List<String> highlights;
  final List<ReminderSnapshot> recentReminders;
  final List<HealthRecordSnapshot> recentHealthRecords;
  final List<DailyLogSnapshot> recentDailyLogs;
}
