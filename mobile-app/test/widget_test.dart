import 'package:flutter_test/flutter_test.dart';
import 'package:petlife_mobile_app/app/pet_life_app.dart';
import 'package:petlife_mobile_app/shared/domain/models/auth_sms_send_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_report_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_report_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/family_invitation_preview_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/health_record_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/home_pet_report_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/media_asset_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/notification_inbox_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/timeline_event_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/user_settings_snapshot.dart';
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
  Future<AuthSmsSendSnapshot> sendLoginSmsCode({
    required String mobile,
  }) async {
    return AuthSmsSendSnapshot(
      mobile: mobile,
      scene: 'login',
      mockedCode: '123456',
      expiresInSeconds: 300,
      resendInSeconds: 60,
    );
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
      mobile: '13800000000',
      nickname: 'Momo',
      familyName: 'Momo Family',
      cityCode: '310000',
      cityName: '上海',
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
  Future<UserSettingsSnapshot> getUserSettings() async {
    return const UserSettingsSnapshot(
      userId: '10001',
      mobile: '13800000000',
      nickname: 'Momo',
      cityCode: '310000',
      cityName: '上海',
      currentPetId: '10001',
      notificationEnabled: true,
      privacyLevel: 'normal',
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
  Future<PetDetailSnapshot> getPet(String petId) async {
    return const PetDetailSnapshot(
      petId: '10001',
      petName: 'Momo',
      petType: 'cat',
      breed: 'British Shorthair',
      gender: 'female',
      neuterStatus: 'completed',
      weightKg: '4.6',
      allergyNotes: '对海鲜较敏感',
      medicalHistory: '2025 年做过牙结石清理。',
    );
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
      weightKg: draft.weightKg,
      allergyNotes: draft.allergyNotes,
      medicalHistory: draft.medicalHistory,
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
      weightKg: draft.weightKg,
      allergyNotes: draft.allergyNotes,
      medicalHistory: draft.medicalHistory,
    );
  }

  @override
  Future<void> archivePet({
    required String petId,
    required String archiveStatus,
  }) async {}

  @override
  Future<void> deletePet(String petId) async {}

  @override
  Future<CurrentUserSnapshot> updateCurrentPet(String petId) async {
    return const CurrentUserSnapshot(
      userId: '10001',
      mobile: '13800000000',
      nickname: 'Momo',
      familyName: 'Momo Family',
      cityCode: '310000',
      cityName: '上海',
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
  Future<UserSettingsSnapshot> updateUserProfile({
    required String nickname,
  }) async {
    return UserSettingsSnapshot(
      userId: '10001',
      mobile: '13800000000',
      nickname: nickname,
      cityCode: '310000',
      cityName: '上海',
      currentPetId: '10001',
      notificationEnabled: true,
      privacyLevel: 'normal',
    );
  }

  @override
  Future<UserSettingsSnapshot> updateUserCity({
    required String cityCode,
    required String cityName,
  }) async {
    return UserSettingsSnapshot(
      userId: '10001',
      mobile: '13800000000',
      nickname: 'Momo',
      cityCode: cityCode,
      cityName: cityName,
      currentPetId: '10001',
      notificationEnabled: true,
      privacyLevel: 'normal',
    );
  }

  @override
  Future<UserSettingsSnapshot> updateNotificationSettings({
    required bool notificationEnabled,
    required String privacyLevel,
  }) async {
    return UserSettingsSnapshot(
      userId: '10001',
      mobile: '13800000000',
      nickname: 'Momo',
      cityCode: '310000',
      cityName: '上海',
      currentPetId: '10001',
      notificationEnabled: notificationEnabled,
      privacyLevel: privacyLevel,
    );
  }

  @override
  Future<NotificationInboxSnapshot> listNotifications({
    String notifyType = 'all',
    String readStatus = 'all',
  }) async {
    final List<NotificationMessageSnapshot> messages =
        <NotificationMessageSnapshot>[
      NotificationMessageSnapshot(
        notificationId: '90001',
        notifyType: 'system',
        bizType: 'user_welcome',
        bizId: '10001',
        title: '欢迎来到宠物生活管家',
        content: '我们会把宠物档案、提醒、日常和重要消息整理在这里。',
        read: false,
        sentAt: DateTime(2026, 4, 24, 9),
      ),
      NotificationMessageSnapshot(
        notificationId: '90002',
        notifyType: 'reminder',
        bizType: 'reminder_completed',
        bizId: '40001',
        title: '提醒已完成',
        content: 'Momo 的「体内驱虫提醒」已完成。',
        read: true,
        sentAt: DateTime(2026, 4, 24, 10),
        readAt: DateTime(2026, 4, 24, 10, 5),
      ),
    ];
    final List<NotificationMessageSnapshot> filteredMessages = messages
        .where((NotificationMessageSnapshot message) =>
            notifyType == 'all' || message.notifyType == notifyType)
        .where((NotificationMessageSnapshot message) {
      if (readStatus == 'unread') {
        return !message.read;
      }
      if (readStatus == 'read') {
        return message.read;
      }
      return true;
    }).toList();
    return NotificationInboxSnapshot(
      items: filteredMessages,
      unreadCount: messages
          .where((NotificationMessageSnapshot message) => !message.read)
          .length,
      systemUnreadCount: messages
          .where((NotificationMessageSnapshot message) =>
              message.notifyType == 'system' && !message.read)
          .length,
      reminderUnreadCount: messages
          .where((NotificationMessageSnapshot message) =>
              message.notifyType == 'reminder' && !message.read)
          .length,
    );
  }

  @override
  Future<NotificationMessageSnapshot> markNotificationRead(
      String notificationId) async {
    return NotificationMessageSnapshot(
      notificationId: notificationId,
      notifyType: 'system',
      bizType: 'user_welcome',
      bizId: '10001',
      title: '欢迎来到宠物生活管家',
      content: '我们会把宠物档案、提醒、日常和重要消息整理在这里。',
      read: true,
      sentAt: DateTime(2026, 4, 24, 9),
      readAt: DateTime(2026, 4, 24, 9, 10),
    );
  }

  @override
  Future<NotificationInboxSnapshot> markNotificationsRead({
    String notifyType = 'all',
  }) async {
    return const NotificationInboxSnapshot(
      items: <NotificationMessageSnapshot>[],
      unreadCount: 0,
      systemUnreadCount: 0,
      reminderUnreadCount: 0,
    );
  }

  @override
  Future<MediaAssetSnapshot> uploadMediaAsset({
    required String bizType,
    required String filePath,
  }) async {
    return MediaAssetSnapshot(
      assetId: '70001',
      bizType: bizType,
      mediaType: 'image',
      fileName: 'momo.jpg',
      fileSize: 1024,
      uploadStatus: 'uploaded',
      reviewStatus: 'pending_review',
      accessUrl: '/api/v1/media-assets/70001/content',
      createdAt: DateTime(2026, 4, 30, 10),
      completedAt: DateTime(2026, 4, 30, 10),
    );
  }

  @override
  Future<MediaAssetSnapshot> getMediaAsset(String assetId) async {
    return MediaAssetSnapshot(
      assetId: assetId,
      bizType: 'daily_log',
      mediaType: 'image',
      fileName: 'momo.jpg',
      fileSize: 1024,
      uploadStatus: 'uploaded',
      reviewStatus: 'pending_review',
      accessUrl: '/api/v1/media-assets/$assetId/content',
      createdAt: DateTime(2026, 4, 30, 10),
      completedAt: DateTime(2026, 4, 30, 10),
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
        syncToCommunity: false,
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
      syncToCommunity: false,
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
      syncToCommunity: draft.syncToCommunity,
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
      syncToCommunity: draft.syncToCommunity,
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
  Future<List<CommunityPostSnapshot>> listCommunityFeed({
    String tab = 'recommended',
  }) async {
    return const <CommunityPostSnapshot>[
      CommunityPostSnapshot(
        postId: '70001',
        postType: 'experience',
        title: '今天第一次主动跳上窗台晒太阳',
        content: '今天第一次主动跳上窗台晒太阳，看起来对家里的环境更放松了。',
        sourceDailyLogId: '50001',
        visibility: 'public',
        likeCount: 8,
        commentCount: 2,
        favoriteCount: 3,
        liked: false,
        favorited: false,
        author: CommunityAuthorSnapshot(
          userId: '10001',
          nickname: 'Momo',
        ),
        pet: CommunityPetSnapshot(
          petId: '10001',
          petName: 'Momo',
          petType: 'cat',
          breed: 'British Shorthair',
        ),
      ),
    ];
  }

  @override
  Future<CommunityPostSnapshot> getCommunityPost(String postId) async {
    return const CommunityPostSnapshot(
      postId: '70001',
      postType: 'experience',
      title: '今天第一次主动跳上窗台晒太阳',
      content: '今天第一次主动跳上窗台晒太阳，看起来对家里的环境更放松了。',
      sourceDailyLogId: '50001',
      visibility: 'public',
      likeCount: 8,
      commentCount: 2,
      favoriteCount: 3,
      liked: false,
      favorited: false,
      author: CommunityAuthorSnapshot(
        userId: '10001',
        nickname: 'Momo',
      ),
      pet: CommunityPetSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
      ),
    );
  }

  @override
  Future<List<CommunityCommentSnapshot>> listCommunityComments(
      String postId) async {
    return const <CommunityCommentSnapshot>[
      CommunityCommentSnapshot(
        commentId: '71001',
        postId: '70001',
        content: '这条观察很真实，能看出已经越来越放松了。',
        author: CommunityAuthorSnapshot(
          userId: '10002',
          nickname: '奶糖',
        ),
      ),
    ];
  }

  @override
  Future<CommunityCommentSnapshot> createCommunityComment({
    required String postId,
    required String content,
  }) async {
    return CommunityCommentSnapshot(
      commentId: '71002',
      postId: postId,
      content: content,
      author: const CommunityAuthorSnapshot(
        userId: '10001',
        nickname: 'Momo',
      ),
      createdAt: DateTime(2026, 4, 22, 13, 30),
    );
  }

  @override
  Future<CommunityPostSnapshot> likeCommunityPost(String postId) async {
    return (await getCommunityPost(postId)).copyWith(
      liked: true,
      likeCount: 9,
    );
  }

  @override
  Future<CommunityPostSnapshot> unlikeCommunityPost(String postId) async {
    return (await getCommunityPost(postId)).copyWith(
      liked: false,
      likeCount: 7,
    );
  }

  @override
  Future<CommunityPostSnapshot> favoriteCommunityPost(String postId) async {
    return (await getCommunityPost(postId)).copyWith(
      favorited: true,
      favoriteCount: 4,
    );
  }

  @override
  Future<CommunityPostSnapshot> unfavoriteCommunityPost(String postId) async {
    return (await getCommunityPost(postId)).copyWith(
      favorited: false,
      favoriteCount: 2,
    );
  }

  @override
  Future<CommunityReportSnapshot> reportCommunityPost({
    required String postId,
    required CommunityReportDraft draft,
  }) async {
    return CommunityReportSnapshot(
      reportId: '72001',
      targetType: 'post',
      targetId: postId,
      reasonCode: draft.reasonCode,
      reasonDetail: draft.reasonDetail,
      status: 'pending',
      createdAt: DateTime(2026, 4, 22, 14, 0),
    );
  }

  @override
  Future<ServiceHomeSnapshot> getServiceHome({
    String? petId,
    String? cityCode,
  }) async {
    return ServiceHomeSnapshot(
      cityCode: cityCode ?? '310000',
      cityName: '上海',
      opened: true,
      commercePlaceholder: '商城当前保持预留，不进入服务预约链路',
      categories: const <ServiceCategorySnapshot>[
        ServiceCategorySnapshot(
          providerType: 'hospital',
          title: '宠物医院',
          description: '体检、疫苗、复诊和异常就医预约。',
          providerCount: 1,
          available: true,
        ),
      ],
      featuredProviders: await listServiceProviders(providerType: 'hospital'),
      upcomingAppointments: const <ServiceAppointmentSnapshot>[],
    );
  }

  @override
  Future<List<ServiceProviderSnapshot>> listServiceProviders({
    String? providerType,
    String? cityCode,
  }) async {
    return <ServiceProviderSnapshot>[
      ServiceProviderSnapshot(
        providerId: '80001',
        providerType: providerType ?? 'hospital',
        providerName: '安心宠物医院',
        cityCode: cityCode ?? '310000',
        address: '上海市徐汇区宠物友好路 88 号',
        contactPhone: '021-12345678',
        businessHours: '09:00-20:00',
        ratingAvg: '4.8',
        reviewCount: 16,
        status: 'online',
        bookable: true,
        serviceItems: const <ProviderServiceItemSnapshot>[
          ProviderServiceItemSnapshot(
            serviceItemId: '81001',
            serviceCode: 'basic',
            serviceName: '基础问诊',
            status: 'active',
            priceMin: '99.00',
            priceMax: '199.00',
          ),
        ],
        availableSlots: <ProviderScheduleSlotSnapshot>[
          ProviderScheduleSlotSnapshot(
            slotId: '82001',
            providerId: '80001',
            appointmentType: providerType ?? 'hospital',
            slotDate: DateTime(2026, 4, 28),
            startTime: '10:00',
            endTime: '11:00',
            quota: 2,
            bookedCount: 0,
            availableQuota: 2,
            status: 'open',
            bookable: true,
          ),
        ],
      ),
    ];
  }

  @override
  Future<ServiceProviderSnapshot> getServiceProvider(String providerId) async {
    return (await listServiceProviders(providerType: 'hospital')).first;
  }

  @override
  Future<List<ProviderScheduleSlotSnapshot>> listProviderSlots({
    required String providerId,
    required String appointmentType,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    return <ProviderScheduleSlotSnapshot>[
      ProviderScheduleSlotSnapshot(
        slotId: '82001',
        providerId: providerId,
        appointmentType: appointmentType,
        slotDate: startDate,
        startTime: '10:00',
        endTime: '11:00',
        quota: 2,
        bookedCount: 0,
        availableQuota: 2,
        status: 'open',
        bookable: true,
      ),
    ];
  }

  @override
  Future<List<ProviderReviewSnapshot>> listProviderReviews({
    required String providerId,
  }) async {
    return <ProviderReviewSnapshot>[
      ProviderReviewSnapshot(
        reviewId: '84001',
        providerId: providerId,
        providerName: '安心宠物医院',
        providerType: 'hospital',
        appointmentId: '83001',
        userId: '10001',
        reviewerNickname: 'Momo家长',
        petId: '10001',
        petName: 'Momo',
        rating: 5,
        content: '医生沟通很细心，复查建议也清楚。',
        status: 'visible',
        createdAt: DateTime(2026, 4, 28),
        updatedAt: DateTime(2026, 4, 28),
      ),
    ];
  }

  @override
  Future<ServiceAppointmentSnapshot> createServiceAppointment(
      ServiceAppointmentDraft draft) async {
    return ServiceAppointmentSnapshot(
      appointmentId: '83001',
      petId: draft.petId,
      petName: 'Momo',
      providerId: draft.providerId,
      providerName: '安心宠物医院',
      providerType: draft.appointmentType,
      appointmentType: draft.appointmentType,
      appointmentDate: draft.appointmentDate,
      appointmentSlot: draft.appointmentSlot,
      demandDesc: draft.demandDesc,
      contactName: draft.contactName,
      contactMobile: draft.contactMobile,
      status: 'pending_confirm',
      reviewed: false,
    );
  }

  @override
  Future<List<ServiceAppointmentSnapshot>> listServiceAppointments({
    String status = 'all',
  }) async {
    return <ServiceAppointmentSnapshot>[
      ServiceAppointmentSnapshot(
        appointmentId: '83001',
        petId: '10001',
        petName: 'Momo',
        providerId: '80001',
        providerName: '安心宠物医院',
        providerType: 'hospital',
        appointmentType: 'hospital',
        appointmentDate: DateTime(2026, 4, 28),
        appointmentSlot: '10:00-11:00',
        contactName: 'Momo家长',
        contactMobile: '13800000000',
        status: 'pending_confirm',
        reviewed: false,
      ),
    ];
  }

  @override
  Future<ServiceAppointmentSnapshot> cancelServiceAppointment({
    required String appointmentId,
    String? cancelReason,
  }) async {
    return ServiceAppointmentSnapshot(
      appointmentId: appointmentId,
      petId: '10001',
      petName: 'Momo',
      providerId: '80001',
      providerName: '安心宠物医院',
      providerType: 'hospital',
      appointmentType: 'hospital',
      appointmentDate: DateTime(2026, 4, 28),
      appointmentSlot: '10:00-11:00',
      contactName: 'Momo家长',
      contactMobile: '13800000000',
      status: 'canceled',
      reviewed: false,
      remark: cancelReason,
    );
  }

  @override
  Future<ProviderReviewSnapshot> createProviderReview({
    required String appointmentId,
    required ServiceReviewDraft draft,
  }) async {
    return ProviderReviewSnapshot(
      reviewId: '84001',
      providerId: '80001',
      providerName: '安心宠物医院',
      providerType: 'hospital',
      appointmentId: appointmentId,
      userId: '10001',
      reviewerNickname: 'Momo家长',
      petId: '10001',
      petName: 'Momo',
      rating: draft.rating,
      content: draft.content,
      status: 'visible',
      createdAt: DateTime(2026, 4, 28),
      updatedAt: DateTime(2026, 4, 28),
    );
  }

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
          syncToCommunity: false,
          happenedAt: DateTime(2026, 4, 17, 8),
          createdAt: DateTime(2026, 4, 17, 8, 5),
        ),
      ],
    );
  }

  @override
  Future<HomePetReportSnapshot> getWeeklyPetReport() async {
    return _buildReportSnapshot('weekly');
  }

  @override
  Future<HomePetReportSnapshot> getMonthlyPetReport() async {
    return _buildReportSnapshot('monthly');
  }

  HomePetReportSnapshot _buildReportSnapshot(String reportType) {
    return HomePetReportSnapshot(
      reportType: reportType,
      pet: const PetDetailSnapshot(
        petId: '10001',
        petName: 'Momo',
        petType: 'cat',
        breed: 'British Shorthair',
        gender: 'female',
        neuterStatus: 'completed',
        weightKg: '4.6',
      ),
      windowStart: reportType == 'monthly'
          ? DateTime(2026, 3, 24, 0)
          : DateTime(2026, 4, 17, 0),
      windowEnd: DateTime(2026, 4, 23, 18),
      pendingReminderCount: 1,
      completedReminderCount: 2,
      skippedReminderCount: 0,
      healthRecordCount: 3,
      dailyLogCount: 4,
      communitySyncCount: 1,
      feedCount: 2,
      waterCount: 3,
      toiletCount: 2,
      weightRecordCount: 1,
      medicationRecordCount: 1,
      highlights: const <String>[
        '这段时间已经完成了 2 条提醒，照护节奏保持得不错。',
        '围绕 Momo 留下了 3 条健康记录。',
      ],
      recentReminders: <ReminderSnapshot>[
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
      ],
      recentHealthRecords: <HealthRecordSnapshot>[
        HealthRecordSnapshot(
          healthRecordId: '30001',
          recordType: 'weight',
          title: '体重复查',
          occurredAt: DateTime(2026, 4, 15, 10),
          value: '4.3',
          unit: 'kg',
          notes: '状态稳定',
        ),
      ],
      recentDailyLogs: <DailyLogSnapshot>[
        DailyLogSnapshot(
          dailyLogId: '50001',
          content: '今天追着逗猫棒跑了十分钟，状态很活跃。',
          tags: const <String>['玩耍', '活跃'],
          visibility: 'family',
          syncToCommunity: false,
          happenedAt: DateTime(2026, 4, 17, 8),
          createdAt: DateTime(2026, 4, 17, 8, 5),
        ),
      ],
    );
  }
}
