import 'package:flutter_test/flutter_test.dart';
import 'package:petlife_mobile_app/app/pet_life_app.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';
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
          dueAt: DateTime(2026, 4, 18, 9),
          status: 'pending',
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
        ),
      ],
    );
  }
}
