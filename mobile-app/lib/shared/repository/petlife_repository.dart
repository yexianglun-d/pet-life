import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_preview_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/health_record_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/timeline_event_snapshot.dart';

/// 用户端仓储抽象。
abstract interface class PetLifeRepository {
  Future<bool> hasLocalSession();

  Future<void> loginBySms({
    required String mobile,
    required String code,
  });

  Future<void> logout();

  Future<CurrentUserSnapshot> getCurrentUser();

  Future<List<PetDetailSnapshot>> listPets();

  Future<PetDetailSnapshot> createPet(PetUpsertDraft draft);

  Future<PetDetailSnapshot> updatePet({
    required String petId,
    required PetUpsertDraft draft,
  });

  Future<CurrentUserSnapshot> updateCurrentPet(String petId);

  Future<List<HealthRecordSnapshot>> listHealthRecords(String petId);

  Future<HealthRecordSnapshot> getHealthRecord({
    required String petId,
    required String healthRecordId,
  });

  Future<HealthRecordSnapshot> createHealthRecord({
    required String petId,
    required HealthRecordDraft draft,
  });

  Future<HealthRecordSnapshot> updateHealthRecord({
    required String petId,
    required String healthRecordId,
    required HealthRecordDraft draft,
  });

  Future<void> deleteHealthRecord({
    required String petId,
    required String healthRecordId,
  });

  Future<List<ReminderSnapshot>> listReminders(String petId);

  Future<ReminderSnapshot> createReminder({
    required String petId,
    required ReminderDraft draft,
  });

  Future<ReminderSnapshot> completeReminder({
    required String petId,
    required String reminderId,
  });

  Future<ReminderSnapshot> skipReminder({
    required String petId,
    required String reminderId,
  });

  Future<List<DailyLogSnapshot>> listDailyLogs(String petId);

  Future<DailyLogSnapshot> getDailyLog({
    required String petId,
    required String dailyLogId,
  });

  Future<DailyLogSnapshot> createDailyLog({
    required String petId,
    required DailyLogDraft draft,
  });

  Future<DailyLogSnapshot> updateDailyLog({
    required String petId,
    required String dailyLogId,
    required DailyLogDraft draft,
  });

  Future<void> deleteDailyLog({
    required String petId,
    required String dailyLogId,
  });

  Future<List<TimelineEventSnapshot>> listTimelineEvents({
    required String petId,
    String eventType = 'all',
  });

  Future<FamilyDetailSnapshot> getFamilyDetail();

  Future<FamilyInvitationSnapshot> createFamilyInvitation(
      FamilyInvitationDraft draft);

  Future<FamilyInvitationPreviewSnapshot> getFamilyInvitationPreview(
      String inviteCode);

  Future<FamilyDetailSnapshot> acceptFamilyInvitation(String inviteCode);

  Future<FamilyInvitationPreviewSnapshot> rejectFamilyInvitation(
      String inviteCode);

  Future<FamilyMemberSnapshot> updateFamilyMemberRole({
    required String memberId,
    required String role,
  });

  Future<void> removeFamilyMember(String memberId);

  Future<PetDashboardSnapshot> getPetDashboard(String petId);
}
