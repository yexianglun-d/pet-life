import 'package:flutter_test/flutter_test.dart';
import 'package:petlife_mobile_app/app/pet_life_app.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_preview_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/health_record_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/timeline_event_snapshot.dart';
import 'package:petlife_mobile_app/shared/repository/petlife_repository.dart';

void main() {
  testWidgets('pet life app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(
      PetLifeApp(repository: _FakePetLifeRepository()),
    );
    await tester.pumpAndSettle();

    expect(find.text('宠物生活管家'), findsOneWidget);
    expect(find.text('Momo'), findsOneWidget);
  });
}

class _FakePetLifeRepository implements PetLifeRepository {
  @override
  Future<bool> hasLocalSession() async {
    return true;
  }

  @override
  Future<void> loginBySms({
    required String mobile,
    required String code,
  }) async {}

  @override
  Future<void> logout() async {}

  @override
  Future<CurrentUserSnapshot> getCurrentUser() async {
    return const CurrentUserSnapshot(
      userId: '10001',
      nickname: 'Momo',
      familyName: 'Momo Family',
      currentPetId: '10001',
      currentPet: PetProfileSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
        gender: 'female',
      ),
    );
  }

  @override
  Future<List<PetDetailSnapshot>> listPets() async {
    return const <PetDetailSnapshot>[
      PetDetailSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
        gender: 'female',
        neuterStatus: 'completed',
      ),
    ];
  }

  @override
  Future<PetDetailSnapshot> createPet(PetUpsertDraft draft) async {
    return PetDetailSnapshot(
      petId: '10002',
      petName: draft.petName,
      petType: draft.petType,
      breed: draft.breed,
      gender: draft.gender,
      neuterStatus: draft.neuterStatus,
      birthday: draft.birthday,
      adoptDate: draft.adoptDate,
    );
  }

  @override
  Future<PetDetailSnapshot> updatePet({
    required String petId,
    required PetUpsertDraft draft,
  }) async {
    return PetDetailSnapshot(
      petId: petId,
      petName: draft.petName,
      petType: draft.petType,
      breed: draft.breed,
      gender: draft.gender,
      neuterStatus: draft.neuterStatus,
      birthday: draft.birthday,
      adoptDate: draft.adoptDate,
    );
  }

  @override
  Future<CurrentUserSnapshot> updateCurrentPet(String petId) async {
    return const CurrentUserSnapshot(
      userId: '10001',
      nickname: 'Momo',
      familyName: 'Momo Family',
      currentPetId: '10001',
      currentPet: PetProfileSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
        gender: 'female',
      ),
    );
  }

  @override
  Future<List<HealthRecordSnapshot>> listHealthRecords(String petId) async {
    return <HealthRecordSnapshot>[
      HealthRecordSnapshot(
        healthRecordId: '30001',
        recordType: 'examination',
        title: '年度体检',
        occurredAt: DateTime(2026, 4, 15, 10),
        notes: '状态稳定',
        createdAt: DateTime(2026, 4, 15, 10, 30),
      ),
    ];
  }

  @override
  Future<HealthRecordSnapshot> getHealthRecord({
    required String petId,
    required String healthRecordId,
  }) async {
    return HealthRecordSnapshot(
      healthRecordId: healthRecordId,
      recordType: 'examination',
      title: '年度体检',
      occurredAt: DateTime(2026, 4, 15, 10),
      value: '4.6',
      unit: 'kg',
      notes: '状态稳定',
      createdAt: DateTime(2026, 4, 15, 10, 30),
    );
  }

  @override
  Future<HealthRecordSnapshot> createHealthRecord({
    required String petId,
    required HealthRecordDraft draft,
  }) async {
    return HealthRecordSnapshot(
      healthRecordId: '30002',
      recordType: draft.recordType,
      title: draft.title,
      occurredAt: draft.occurredAt,
      value: draft.value,
      unit: draft.unit,
      notes: draft.notes,
      createdAt: DateTime(2026, 4, 21, 18),
    );
  }

  @override
  Future<HealthRecordSnapshot> updateHealthRecord({
    required String petId,
    required String healthRecordId,
    required HealthRecordDraft draft,
  }) async {
    return HealthRecordSnapshot(
      healthRecordId: healthRecordId,
      recordType: draft.recordType,
      title: draft.title,
      occurredAt: draft.occurredAt,
      value: draft.value,
      unit: draft.unit,
      notes: draft.notes,
      createdAt: DateTime(2026, 4, 21, 18),
    );
  }

  @override
  Future<void> deleteHealthRecord({
    required String petId,
    required String healthRecordId,
  }) async {}

  @override
  Future<List<ReminderSnapshot>> listReminders(String petId) async {
    return <ReminderSnapshot>[
      ReminderSnapshot(
        reminderId: '40001',
        reminderType: 'deworming',
        title: '体内驱虫提醒',
        reminderMode: 'cycle',
        dueAt: DateTime(2026, 4, 18, 9),
        status: 'pending',
        cycleValue: 1,
        cycleUnit: 'month',
        notes: '晚饭后执行',
      ),
    ];
  }

  @override
  Future<ReminderSnapshot> createReminder({
    required String petId,
    required ReminderDraft draft,
  }) async {
    return ReminderSnapshot(
      reminderId: '40002',
      reminderType: draft.reminderType,
      title: draft.title,
      reminderMode: draft.reminderMode,
      dueAt: draft.dueAt,
      status: 'pending',
      cycleValue: draft.cycleValue,
      cycleUnit: draft.cycleUnit,
      notes: draft.notes,
    );
  }

  @override
  Future<ReminderSnapshot> completeReminder({
    required String petId,
    required String reminderId,
  }) async {
    return ReminderSnapshot(
      reminderId: reminderId,
      reminderType: 'deworming',
      title: '体内驱虫提醒',
      reminderMode: 'cycle',
      dueAt: DateTime(2026, 4, 18, 9),
      status: 'completed',
      cycleValue: 1,
      cycleUnit: 'month',
      notes: '晚饭后执行',
    );
  }

  @override
  Future<ReminderSnapshot> skipReminder({
    required String petId,
    required String reminderId,
  }) async {
    return ReminderSnapshot(
      reminderId: reminderId,
      reminderType: 'deworming',
      title: '体内驱虫提醒',
      reminderMode: 'cycle',
      dueAt: DateTime(2026, 4, 18, 9),
      status: 'skipped',
      cycleValue: 1,
      cycleUnit: 'month',
      notes: '晚饭后执行',
    );
  }

  @override
  Future<List<DailyLogSnapshot>> listDailyLogs(String petId) async {
    return <DailyLogSnapshot>[
      DailyLogSnapshot(
        dailyLogId: '50001',
        content: '今天追着逗猫棒跑了十分钟，状态很活跃。',
        tags: const <String>['玩耍', '活跃'],
        visibility: 'family',
        happenedAt: DateTime(2026, 4, 17, 8),
        createdAt: DateTime(2026, 4, 17, 8, 5),
      ),
    ];
  }

  @override
  Future<DailyLogSnapshot> getDailyLog({
    required String petId,
    required String dailyLogId,
  }) async {
    return DailyLogSnapshot(
      dailyLogId: dailyLogId,
      content: '今天追着逗猫棒跑了十分钟，状态很活跃。',
      tags: const <String>['玩耍', '活跃'],
      visibility: 'family',
      happenedAt: DateTime(2026, 4, 17, 8),
      createdAt: DateTime(2026, 4, 17, 8, 5),
    );
  }

  @override
  Future<DailyLogSnapshot> createDailyLog({
    required String petId,
    required DailyLogDraft draft,
  }) async {
    return DailyLogSnapshot(
      dailyLogId: '50002',
      content: draft.content,
      tags: draft.tags,
      visibility: draft.visibility,
      happenedAt: draft.happenedAt,
      createdAt: DateTime(2026, 4, 21, 18),
    );
  }

  @override
  Future<DailyLogSnapshot> updateDailyLog({
    required String petId,
    required String dailyLogId,
    required DailyLogDraft draft,
  }) async {
    return DailyLogSnapshot(
      dailyLogId: dailyLogId,
      content: draft.content,
      tags: draft.tags,
      visibility: draft.visibility,
      happenedAt: draft.happenedAt,
      createdAt: DateTime(2026, 4, 21, 18),
    );
  }

  @override
  Future<void> deleteDailyLog({
    required String petId,
    required String dailyLogId,
  }) async {}

  @override
  Future<List<TimelineEventSnapshot>> listTimelineEvents({
    required String petId,
    String eventType = 'all',
  }) async {
    final List<TimelineEventSnapshot> events = <TimelineEventSnapshot>[
      TimelineEventSnapshot(
        eventId: '60002',
        eventType: 'daily_log',
        sourceType: 'daily_log',
        sourceId: '50001',
        eventTime: DateTime(2026, 4, 17, 8),
        title: '今天追着逗猫棒跑了十分钟...',
        summary: '今天追着逗猫棒跑了十分钟，状态很活跃。',
        visibility: 'family',
      ),
      TimelineEventSnapshot(
        eventId: '60001',
        eventType: 'health',
        sourceType: 'health_record',
        sourceId: '30001',
        eventTime: DateTime(2026, 4, 15, 10),
        title: '年度体检',
        summary: '4.6 kg · 状态稳定',
        visibility: 'family',
      ),
    ];
    if (eventType == 'all') {
      return events;
    }
    return events
        .where((TimelineEventSnapshot event) => event.eventType == eventType)
        .toList();
  }

  @override
  Future<FamilyDetailSnapshot> getFamilyDetail() async {
    return FamilyDetailSnapshot(
      familyId: '20001',
      familyName: 'Momo Family',
      memberCount: 2,
      currentUserRole: 'owner',
      members: const <FamilyMemberSnapshot>[
        FamilyMemberSnapshot(
          memberId: '21001',
          userId: '10001',
          nickname: 'Momo',
          mobile: '13800000000',
          role: 'owner',
          inviteStatus: 'joined',
        ),
        FamilyMemberSnapshot(
          memberId: '21002',
          userId: '10002',
          nickname: '奶糖',
          mobile: '13900000000',
          role: 'member',
          inviteStatus: 'joined',
        ),
      ],
      sharedPets: const <FamilySharedPetSnapshot>[
        FamilySharedPetSnapshot(
          petId: '10001',
          petName: 'Momo',
          petType: 'cat',
          breed: 'British Shorthair',
        ),
      ],
      pendingInvitations: const <FamilyInvitationSnapshot>[
        FamilyInvitationSnapshot(
          invitationId: '22001',
          inviteeMobile: '13700000000',
          role: 'member',
          sharedPetIds: <String>['10001'],
          inviteCode: 'invite-code-001',
          status: 'pending',
        ),
      ],
    );
  }

  @override
  Future<FamilyInvitationSnapshot> createFamilyInvitation(
    FamilyInvitationDraft draft,
  ) async {
    return FamilyInvitationSnapshot(
      invitationId: '22002',
      inviteeMobile: draft.inviteeMobile,
      role: draft.role,
      sharedPetIds: draft.sharedPetIds,
      inviteCode: 'invite-code-002',
      status: 'pending',
      createdAt: DateTime(2026, 4, 21, 18, 30),
    );
  }

  @override
  Future<FamilyInvitationPreviewSnapshot> getFamilyInvitationPreview(
      String inviteCode) async {
    return const FamilyInvitationPreviewSnapshot(
      invitationId: '22001',
      familyId: '20001',
      familyName: 'Momo Family',
      inviterNickname: 'Momo',
      inviteeMobile: '13700000000',
      role: 'member',
      sharedPets: <FamilySharedPetSnapshot>[
        FamilySharedPetSnapshot(
          petId: '10001',
          petName: 'Momo',
          petType: 'cat',
          breed: 'British Shorthair',
        ),
      ],
      inviteCode: 'JOINMO01',
      status: 'pending',
    );
  }

  @override
  Future<FamilyDetailSnapshot> acceptFamilyInvitation(String inviteCode) async {
    return getFamilyDetail();
  }

  @override
  Future<FamilyInvitationPreviewSnapshot> rejectFamilyInvitation(
      String inviteCode) async {
    return const FamilyInvitationPreviewSnapshot(
      invitationId: '22001',
      familyId: '20001',
      familyName: 'Momo Family',
      inviterNickname: 'Momo',
      inviteeMobile: '13700000000',
      role: 'member',
      sharedPets: <FamilySharedPetSnapshot>[
        FamilySharedPetSnapshot(
          petId: '10001',
          petName: 'Momo',
          petType: 'cat',
          breed: 'British Shorthair',
        ),
      ],
      inviteCode: 'JOINMO01',
      status: 'rejected',
    );
  }

  @override
  Future<FamilyMemberSnapshot> updateFamilyMemberRole({
    required String memberId,
    required String role,
  }) async {
    return FamilyMemberSnapshot(
      memberId: memberId,
      userId: '10002',
      nickname: '奶糖',
      mobile: '13900000000',
      role: role,
      inviteStatus: 'joined',
      joinedAt: DateTime(2026, 4, 20, 9),
    );
  }

  @override
  Future<void> removeFamilyMember(String memberId) async {}

  @override
  Future<PetDashboardSnapshot> getPetDashboard(String petId) async {
    return PetDashboardSnapshot(
      pet: const PetProfileSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
        gender: 'female',
      ),
      todayTodoCount: 2,
      reminders: <ReminderSnapshot>[
        ReminderSnapshot(
          reminderId: '40001',
          reminderType: 'deworming',
          title: '体内驱虫提醒',
          reminderMode: 'cycle',
          dueAt: DateTime(2026, 4, 18, 9),
          status: 'pending',
          cycleValue: 1,
          cycleUnit: 'month',
        ),
      ],
      healthRecords: <HealthRecordSnapshot>[
        HealthRecordSnapshot(
          healthRecordId: '30001',
          recordType: 'weight',
          title: '体重复查',
          occurredAt: DateTime(2026, 4, 15, 10),
          value: '4.3',
          unit: 'kg',
        ),
      ],
      dailyLogs: <DailyLogSnapshot>[
        DailyLogSnapshot(
          dailyLogId: '50001',
          content: '今天追着逗猫棒跑了十分钟，状态很活跃。',
          tags: const <String>['玩耍', '活跃'],
          visibility: 'family',
          happenedAt: DateTime(2026, 4, 17, 8),
          createdAt: DateTime(2026, 4, 17, 8, 5),
        ),
      ],
    );
  }
}
