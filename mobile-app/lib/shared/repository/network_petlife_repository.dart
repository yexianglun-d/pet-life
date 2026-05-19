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
import 'package:petlife_mobile_app/shared/domain/models/home_aggregate_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/home_pet_report_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/media_asset_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/notification_inbox_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/reminder_template_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/timeline_event_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/user_settings_snapshot.dart';
import 'package:petlife_mobile_app/shared/network/api_client.dart';
import 'package:petlife_mobile_app/shared/network/api_exception.dart';
import 'package:petlife_mobile_app/shared/repository/petlife_repository.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 基于 HTTP 的用户端仓储实现。
///
/// 首页和宠物页都依赖多条接口组合数据。仓储层负责聚合调用顺序和 JSON 映射，
/// 页面只消费已经整理好的快照对象，避免展示层直接承担网络协议细节。
class NetworkPetLifeRepository implements PetLifeRepository {
  NetworkPetLifeRepository({
    required ApiClient apiClient,
    required AppSessionStore sessionStore,
  })  : _apiClient = apiClient,
        _sessionStore = sessionStore;

  final ApiClient _apiClient;
  final AppSessionStore _sessionStore;

  @override
  Future<bool> hasLocalSession() {
    return _sessionStore.hasSession();
  }

  @override
  Future<AuthSmsSendSnapshot> sendLoginSmsCode({
    required String mobile,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/auth/sms/send',
        body: <String, Object?>{
          'mobile': mobile,
          'scene': 'login',
        },
      ),
      context: '发送验证码响应',
    );

    return AuthSmsSendSnapshot(
      mobile: _readString(data, 'mobile'),
      scene: _readString(data, 'scene'),
      mockedCode: _readString(data, 'mocked_code'),
      expiresInSeconds: _readInt(data, 'expires_in_seconds'),
      resendInSeconds: _readInt(data, 'resend_in_seconds'),
    );
  }

  @override
  Future<void> loginBySms({
    required String mobile,
    required String code,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/auth/login/sms',
        body: <String, Object?>{
          'mobile': mobile,
          'code': code,
        },
      ),
      context: '登录响应',
    );

    await _sessionStore.saveSession(
      accessToken: _readString(data, 'access_token'),
      refreshToken: _readString(data, 'refresh_token'),
    );
  }

  @override
  Future<void> logout() async {
    final String? refreshToken = await _sessionStore.readRefreshToken();
    if (refreshToken != null) {
      await _apiClient.postData(
        '/api/v1/auth/logout',
        body: <String, Object?>{
          'refresh_token': refreshToken,
        },
      );
    }
    await _sessionStore.clear();
  }

  @override
  Future<CurrentUserSnapshot> getCurrentUser() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/me'),
      context: '当前用户数据',
    );
    return _toCurrentUserSnapshot(data);
  }

  @override
  Future<UserSettingsSnapshot> getUserSettings() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/me/settings'),
      context: '用户设置数据',
    );
    return _toUserSettingsSnapshot(data);
  }

  @override
  Future<List<PetDetailSnapshot>> listPets() async {
    final List<Map<String, dynamic>> pets = _asMapList(
      await _apiClient.getData('/api/v1/pets'),
      context: '宠物列表',
    );

    return pets.map(_toPetDetailSnapshot).toList();
  }

  @override
  Future<PetDetailSnapshot> getPet(String petId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/pets/$petId'),
      context: '宠物详情',
    );
    return _toPetDetailSnapshot(data);
  }

  @override
  Future<PetDetailSnapshot> createPet(PetUpsertDraft draft) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/pets',
        body: _toPetUpsertBody(draft),
      ),
      context: '创建宠物响应',
    );
    return _toPetDetailSnapshot(data);
  }

  @override
  Future<PetDetailSnapshot> updatePet({
    required String petId,
    required PetUpsertDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/pets/$petId',
        body: _toPetUpsertBody(draft),
      ),
      context: '编辑宠物响应',
    );
    return _toPetDetailSnapshot(data);
  }

  @override
  Future<void> archivePet({
    required String petId,
    required String archiveStatus,
  }) async {
    await _apiClient.patchData(
      '/api/v1/pets/$petId/archive',
      body: <String, Object?>{
        'archive_status': archiveStatus,
      },
    );
  }

  @override
  Future<void> deletePet(String petId) async {
    await _apiClient.deleteData('/api/v1/pets/$petId');
  }

  @override
  Future<CurrentUserSnapshot> updateCurrentPet(String petId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/me/settings/current-pet',
        body: <String, Object?>{
          'pet_id': petId,
        },
      ),
      context: '切换当前宠物响应',
    );
    return _toCurrentUserSnapshot(data);
  }

  @override
  Future<UserSettingsSnapshot> updateUserProfile({
    required String nickname,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/me/profile',
        body: <String, Object?>{
          'nickname': nickname,
        },
      ),
      context: '用户资料更新响应',
    );
    return _toUserSettingsSnapshot(data);
  }

  @override
  Future<UserSettingsSnapshot> updateUserCity({
    required String cityCode,
    required String cityName,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/me/settings/city',
        body: <String, Object?>{
          'city_code': cityCode,
          'city_name': cityName,
        },
      ),
      context: '城市设置更新响应',
    );
    return _toUserSettingsSnapshot(data);
  }

  @override
  Future<UserSettingsSnapshot> updateNotificationSettings({
    required bool notificationEnabled,
    required String privacyLevel,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/me/settings/notifications',
        body: <String, Object?>{
          'notification_enabled': notificationEnabled,
          'privacy_level': privacyLevel,
        },
      ),
      context: '通知设置更新响应',
    );
    return _toUserSettingsSnapshot(data);
  }

  @override
  Future<NotificationInboxSnapshot> listNotifications({
    String notifyType = 'all',
    String readStatus = 'all',
  }) async {
    final Uri uri = Uri(
      path: '/api/v1/notifications',
      queryParameters: <String, String>{
        'notify_type': notifyType,
        'read_status': readStatus,
      },
    );
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData(uri.toString()),
      context: '消息中心数据',
    );
    return _toNotificationInboxSnapshot(data);
  }

  @override
  Future<NotificationMessageSnapshot> markNotificationRead(
      String notificationId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/notifications/$notificationId/read',
        body: const <String, Object?>{},
      ),
      context: '通知已读响应',
    );
    return _toNotificationMessageSnapshot(data);
  }

  @override
  Future<NotificationInboxSnapshot> markNotificationsRead({
    String notifyType = 'all',
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/notifications/read',
        body: <String, Object?>{
          'notify_type': notifyType,
        },
      ),
      context: '批量已读响应',
    );
    return _toNotificationInboxSnapshot(data);
  }

  @override
  Future<MediaAssetSnapshot> uploadMediaAsset({
    required String bizType,
    required String filePath,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postMultipartData(
        '/api/v1/media-assets',
        fields: <String, String>{
          'biz_type': bizType,
        },
        filePath: filePath,
      ),
      context: '媒体上传响应',
    );
    return _toMediaAssetSnapshot(data);
  }

  @override
  Future<MediaAssetSnapshot> getMediaAsset(String assetId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/media-assets/$assetId'),
      context: '媒体资产响应',
    );
    return _toMediaAssetSnapshot(data);
  }

  @override
  Future<List<HealthRecordSnapshot>> listHealthRecords(String petId) async {
    final List<Map<String, dynamic>> healthRecords = _asMapList(
      await _apiClient.getData('/api/v1/pets/$petId/health-records'),
      context: '健康记录列表',
    );

    return healthRecords.map(_toHealthRecordSnapshot).toList();
  }

  @override
  Future<HealthRecordSnapshot> getHealthRecord({
    required String petId,
    required String healthRecordId,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient
          .getData('/api/v1/pets/$petId/health-records/$healthRecordId'),
      context: '健康记录详情',
    );

    return _toHealthRecordSnapshot(data);
  }

  @override
  Future<HealthRecordSnapshot> createHealthRecord({
    required String petId,
    required HealthRecordDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/pets/$petId/health-records',
        body: <String, Object?>{
          'record_type': draft.recordType,
          'title': draft.title,
          'value': draft.value,
          'unit': draft.unit,
          'hospital_name': draft.hospitalName,
          'doctor_name': draft.doctorName,
          'severity_level': draft.severityLevel,
          'result_summary': draft.resultSummary,
          'attachment_asset_ids': draft.attachmentAssetIds,
          'next_reminder_at': draft.nextReminderAt?.toUtc().toIso8601String(),
          'next_reminder_title': draft.nextReminderTitle,
          'occurred_at': draft.occurredAt.toUtc().toIso8601String(),
          'notes': draft.notes,
        },
      ),
      context: '创建健康记录响应',
    );

    return _toHealthRecordSnapshot(data);
  }

  @override
  Future<HealthRecordSnapshot> updateHealthRecord({
    required String petId,
    required String healthRecordId,
    required HealthRecordDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/pets/$petId/health-records/$healthRecordId',
        body: <String, Object?>{
          'record_type': draft.recordType,
          'title': draft.title,
          'value': draft.value,
          'unit': draft.unit,
          'hospital_name': draft.hospitalName,
          'doctor_name': draft.doctorName,
          'severity_level': draft.severityLevel,
          'result_summary': draft.resultSummary,
          'attachment_asset_ids': draft.attachmentAssetIds,
          'next_reminder_at': draft.nextReminderAt?.toUtc().toIso8601String(),
          'next_reminder_title': draft.nextReminderTitle,
          'occurred_at': draft.occurredAt.toUtc().toIso8601String(),
          'notes': draft.notes,
        },
      ),
      context: '编辑健康记录响应',
    );

    return _toHealthRecordSnapshot(data);
  }

  @override
  Future<void> deleteHealthRecord({
    required String petId,
    required String healthRecordId,
  }) async {
    await _apiClient
        .deleteData('/api/v1/pets/$petId/health-records/$healthRecordId');
  }

  @override
  Future<List<ReminderSnapshot>> listReminders(String petId) async {
    final List<Map<String, dynamic>> reminders = _asMapList(
      await _apiClient.getData('/api/v1/pets/$petId/reminders'),
      context: '提醒列表',
    );

    return reminders.map(_toReminderSnapshot).toList();
  }

  @override
  Future<List<ReminderTemplateSnapshot>> listReminderTemplates(
      String petId) async {
    final List<Map<String, dynamic>> templates = _asMapList(
      await _apiClient.getData('/api/v1/pets/$petId/reminder-templates'),
      context: '提醒模板列表',
    );

    return templates.map(_toReminderTemplateSnapshot).toList();
  }

  @override
  Future<ReminderSnapshot> createReminder({
    required String petId,
    required ReminderDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/pets/$petId/reminders',
        body: <String, Object?>{
          'reminder_type': draft.reminderType,
          'title': draft.title,
          'reminder_mode': draft.reminderMode,
          'cycle_value': draft.cycleValue,
          'cycle_unit': draft.cycleUnit,
          'due_at': draft.dueAt.toUtc().toIso8601String(),
          'notes': draft.notes,
        },
      ),
      context: '创建提醒响应',
    );

    return _toReminderSnapshot(data);
  }

  @override
  Future<ReminderSnapshot> completeReminder({
    required String petId,
    required String reminderId,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/pets/$petId/reminders/$reminderId/complete',
        body: const <String, Object?>{},
      ),
      context: '完成提醒响应',
    );

    return _toReminderSnapshot(data);
  }

  @override
  Future<ReminderSnapshot> skipReminder({
    required String petId,
    required String reminderId,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/pets/$petId/reminders/$reminderId/skip',
        body: const <String, Object?>{},
      ),
      context: '跳过提醒响应',
    );

    return _toReminderSnapshot(data);
  }

  @override
  Future<List<DailyLogSnapshot>> listDailyLogs(String petId) async {
    final List<Map<String, dynamic>> dailyLogs = _asMapList(
      await _apiClient.getData('/api/v1/pets/$petId/daily-logs'),
      context: '萌宠日常列表',
    );

    return dailyLogs.map(_toDailyLogSnapshot).toList();
  }

  @override
  Future<DailyLogSnapshot> getDailyLog({
    required String petId,
    required String dailyLogId,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/pets/$petId/daily-logs/$dailyLogId'),
      context: '萌宠日常详情',
    );

    return _toDailyLogSnapshot(data);
  }

  @override
  Future<DailyLogSnapshot> createDailyLog({
    required String petId,
    required DailyLogDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/pets/$petId/daily-logs',
        body: <String, Object?>{
          'content': draft.content,
          'media_asset_ids': draft.mediaAssetIds,
          'tags': draft.tags,
          'visibility': draft.visibility,
          'sync_to_community': draft.syncToCommunity,
          'happened_at': draft.happenedAt.toUtc().toIso8601String(),
        },
      ),
      context: '创建萌宠日常响应',
    );

    return _toDailyLogSnapshot(data);
  }

  @override
  Future<DailyLogSnapshot> updateDailyLog({
    required String petId,
    required String dailyLogId,
    required DailyLogDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/pets/$petId/daily-logs/$dailyLogId',
        body: <String, Object?>{
          'content': draft.content,
          'media_asset_ids': draft.mediaAssetIds,
          'tags': draft.tags,
          'visibility': draft.visibility,
          'sync_to_community': draft.syncToCommunity,
          'happened_at': draft.happenedAt.toUtc().toIso8601String(),
        },
      ),
      context: '编辑萌宠日常响应',
    );

    return _toDailyLogSnapshot(data);
  }

  @override
  Future<void> deleteDailyLog({
    required String petId,
    required String dailyLogId,
  }) async {
    await _apiClient.deleteData('/api/v1/pets/$petId/daily-logs/$dailyLogId');
  }

  @override
  Future<List<CommunityPostSnapshot>> listCommunityFeed({
    String tab = 'recommended',
  }) async {
    final List<Map<String, dynamic>> posts = _asMapList(
      await _apiClient.getData('/api/v1/community/feed?tab=$tab'),
      context: '社区内容流',
    );
    return posts.map(_toCommunityPostSnapshot).toList();
  }

  @override
  Future<CommunityPostSnapshot> createCommunityPost(
      CommunityPostDraft draft) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/posts',
        body: _toCommunityPostBody(draft),
      ),
      context: '社区发帖响应',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityPostSnapshot> getCommunityPost(String postId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/community/posts/$postId'),
      context: '社区帖子详情',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityTopicDetailSnapshot> getCommunityTopic(String topicId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/community/topics/$topicId'),
      context: '社区话题详情',
    );
    return _toCommunityTopicDetailSnapshot(data);
  }

  @override
  Future<CommunityQuestionDetailSnapshot> getCommunityQuestion(
    String questionId,
  ) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/community/questions/$questionId'),
      context: '社区问答详情',
    );
    return _toCommunityQuestionDetailSnapshot(data);
  }

  @override
  Future<CommunityFollowStatusSnapshot> getCommunityFollowStatus(
    String userId,
  ) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/community/users/$userId/follow-status'),
      context: '社区关注状态',
    );
    return _toCommunityFollowStatusSnapshot(data);
  }

  @override
  Future<CommunityFollowStatusSnapshot> followCommunityUser(
      String userId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/users/$userId/follow',
        body: const <String, Object?>{},
      ),
      context: '关注社区用户响应',
    );
    return _toCommunityFollowStatusSnapshot(data);
  }

  @override
  Future<CommunityFollowStatusSnapshot> unfollowCommunityUser(
    String userId,
  ) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.deleteData('/api/v1/community/users/$userId/follow'),
      context: '取消关注社区用户响应',
    );
    return _toCommunityFollowStatusSnapshot(data);
  }

  @override
  Future<List<CommunityCommentSnapshot>> listCommunityComments(
      String postId) async {
    final List<Map<String, dynamic>> comments = _asMapList(
      await _apiClient.getData('/api/v1/community/posts/$postId/comments'),
      context: '社区评论列表',
    );
    return comments.map(_toCommunityCommentSnapshot).toList();
  }

  @override
  Future<CommunityCommentSnapshot> createCommunityComment({
    required String postId,
    required String content,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/posts/$postId/comments',
        body: <String, Object?>{
          'content': content,
        },
      ),
      context: '创建社区评论响应',
    );
    return _toCommunityCommentSnapshot(data);
  }

  @override
  Future<CommunityPostSnapshot> likeCommunityPost(String postId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/posts/$postId/like',
        body: const <String, Object?>{},
      ),
      context: '点赞社区帖子响应',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityPostSnapshot> unlikeCommunityPost(String postId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.deleteData('/api/v1/community/posts/$postId/like'),
      context: '取消点赞社区帖子响应',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityPostSnapshot> favoriteCommunityPost(String postId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/posts/$postId/favorite',
        body: const <String, Object?>{},
      ),
      context: '收藏社区帖子响应',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityPostSnapshot> unfavoriteCommunityPost(String postId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.deleteData('/api/v1/community/posts/$postId/favorite'),
      context: '取消收藏社区帖子响应',
    );
    return _toCommunityPostSnapshot(data);
  }

  @override
  Future<CommunityReportSnapshot> reportCommunityPost({
    required String postId,
    required CommunityReportDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/community/posts/$postId/report',
        body: <String, Object?>{
          'reason_code': draft.reasonCode,
          'reason_detail': draft.reasonDetail,
        },
      ),
      context: '举报社区帖子响应',
    );
    return _toCommunityReportSnapshot(data);
  }

  @override
  Future<ServiceHomeSnapshot> getServiceHome({
    String? petId,
    String? cityCode,
  }) async {
    final Uri uri = Uri(
      path: '/api/v1/services/home',
      queryParameters: <String, String>{
        if (petId != null) 'pet_id': petId,
        if (cityCode != null) 'city_code': cityCode,
      },
    );
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData(uri.toString()),
      context: '服务中心首页',
    );
    return _toServiceHomeSnapshot(data);
  }

  @override
  Future<List<ServiceProviderSnapshot>> listServiceProviders({
    String? providerType,
    String? cityCode,
  }) async {
    final Uri uri = Uri(
      path: '/api/v1/providers',
      queryParameters: <String, String>{
        if (providerType != null) 'provider_type': providerType,
        if (cityCode != null) 'city_code': cityCode,
      },
    );
    final List<Map<String, dynamic>> providers = _asMapList(
      await _apiClient.getData(uri.toString()),
      context: '服务商列表',
    );
    return providers.map(_toServiceProviderSnapshot).toList();
  }

  @override
  Future<ServiceProviderSnapshot> getServiceProvider(String providerId) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/providers/$providerId'),
      context: '服务商详情',
    );
    return _toServiceProviderSnapshot(data);
  }

  @override
  Future<List<ProviderScheduleSlotSnapshot>> listProviderSlots({
    required String providerId,
    required String appointmentType,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    final Uri uri = Uri(
      path: '/api/v1/providers/$providerId/slots',
      queryParameters: <String, String>{
        'appointment_type': appointmentType,
        'start_date': _formatDate(startDate)!,
        'end_date': _formatDate(endDate)!,
      },
    );
    final List<Map<String, dynamic>> slots = _asMapList(
      await _apiClient.getData(uri.toString()),
      context: '服务商时段列表',
    );
    return slots.map(_toProviderScheduleSlotSnapshot).toList();
  }

  @override
  Future<List<ProviderReviewSnapshot>> listProviderReviews({
    required String providerId,
  }) async {
    final List<Map<String, dynamic>> reviews = _asMapList(
      await _apiClient.getData('/api/v1/providers/$providerId/reviews'),
      context: '服务商评价列表',
    );
    return reviews.map(_toProviderReviewSnapshot).toList();
  }

  @override
  Future<ServiceAppointmentSnapshot> createServiceAppointment(
      ServiceAppointmentDraft draft) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/appointments',
        body: <String, Object?>{
          'pet_id': draft.petId,
          'provider_id': draft.providerId,
          'appointment_type': draft.appointmentType,
          'appointment_date': _formatDate(draft.appointmentDate),
          'appointment_slot': draft.appointmentSlot,
          'demand_desc': draft.demandDesc,
          'contact_name': draft.contactName,
          'contact_mobile': draft.contactMobile,
        },
      ),
      context: '创建服务预约响应',
    );
    return _toServiceAppointmentSnapshot(data);
  }

  @override
  Future<List<ServiceAppointmentSnapshot>> listServiceAppointments({
    String status = 'all',
  }) async {
    final Uri uri = Uri(
      path: '/api/v1/appointments',
      queryParameters: <String, String>{'status': status},
    );
    final List<Map<String, dynamic>> appointments = _asMapList(
      await _apiClient.getData(uri.toString()),
      context: '服务预约列表',
    );
    return appointments.map(_toServiceAppointmentSnapshot).toList();
  }

  @override
  Future<ServiceAppointmentSnapshot> cancelServiceAppointment({
    required String appointmentId,
    String? cancelReason,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/appointments/$appointmentId/cancel',
        body: <String, Object?>{
          'cancel_reason': cancelReason,
        },
      ),
      context: '取消服务预约响应',
    );
    return _toServiceAppointmentSnapshot(data);
  }

  @override
  Future<ProviderReviewSnapshot> createProviderReview({
    required String appointmentId,
    required ServiceReviewDraft draft,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/appointments/$appointmentId/review',
        body: <String, Object?>{
          'rating': draft.rating,
          'content': draft.content,
        },
      ),
      context: '创建服务评价响应',
    );
    return _toProviderReviewSnapshot(data);
  }

  @override
  Future<List<TimelineEventSnapshot>> listTimelineEvents({
    required String petId,
    String eventType = 'all',
  }) async {
    final String path = eventType == 'all'
        ? '/api/v1/pets/$petId/timeline'
        : '/api/v1/pets/$petId/timeline?event_type=$eventType';
    final List<Map<String, dynamic>> timelineEvents = _asMapList(
      await _apiClient.getData(path),
      context: '成长时间轴列表',
    );

    return timelineEvents.map(_toTimelineEventSnapshot).toList();
  }

  @override
  Future<FamilyDetailSnapshot> getFamilyDetail() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/family'),
      context: '家庭详情',
    );

    return _toFamilyDetailSnapshot(data);
  }

  @override
  Future<FamilyInvitationSnapshot> createFamilyInvitation(
      FamilyInvitationDraft draft) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/family/invitations',
        body: <String, Object?>{
          'invitee_mobile': draft.inviteeMobile,
          'role': draft.role,
          'shared_pet_ids': draft.sharedPetIds,
        },
      ),
      context: '创建家庭邀请响应',
    );

    return _toFamilyInvitationSnapshot(data);
  }

  @override
  Future<FamilyInvitationPreviewSnapshot> getFamilyInvitationPreview(
      String inviteCode) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/family/invitations/$inviteCode'),
      context: '家庭邀请预览',
    );
    return _toFamilyInvitationPreviewSnapshot(data);
  }

  @override
  Future<FamilyDetailSnapshot> acceptFamilyInvitation(String inviteCode) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/family/invitations/$inviteCode/accept',
        body: const <String, Object?>{},
      ),
      context: '接受家庭邀请响应',
    );
    return _toFamilyDetailSnapshot(data);
  }

  @override
  Future<FamilyInvitationPreviewSnapshot> rejectFamilyInvitation(
      String inviteCode) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.postData(
        '/api/v1/family/invitations/$inviteCode/reject',
        body: const <String, Object?>{},
      ),
      context: '拒绝家庭邀请响应',
    );
    return _toFamilyInvitationPreviewSnapshot(data);
  }

  @override
  Future<FamilyMemberSnapshot> updateFamilyMemberRole({
    required String memberId,
    required String role,
  }) async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.patchData(
        '/api/v1/family/members/$memberId/role',
        body: <String, Object?>{
          'role': role,
        },
      ),
      context: '修改成员角色响应',
    );

    return _toFamilyMemberSnapshot(data);
  }

  @override
  Future<void> removeFamilyMember(String memberId) async {
    await _apiClient.deleteData('/api/v1/family/members/$memberId');
  }

  CurrentUserSnapshot _toCurrentUserSnapshot(Map<String, dynamic> data) {
    final Map<String, dynamic> user = _asMap(data['user'], context: '用户信息');
    final Map<String, dynamic> familySummary =
        _asMap(data['family_summary'], context: '家庭信息');
    final Object? currentPetPayload = data['current_pet'];

    return CurrentUserSnapshot(
      userId: _readString(user, 'user_id'),
      mobile: _readString(user, 'mobile'),
      nickname: _readString(user, 'nickname'),
      familyName: _readString(familySummary, 'family_name'),
      cityCode: _readNullableString(user, 'city_code'),
      cityName: _readNullableString(user, 'city_name'),
      currentPetId: _readNullableString(data, 'current_pet_id'),
      currentPet: currentPetPayload == null
          ? null
          : _toPetProfileSnapshot(
              _asMap(currentPetPayload, context: '当前宠物信息'),
            ),
    );
  }

  UserSettingsSnapshot _toUserSettingsSnapshot(Map<String, dynamic> payload) {
    return UserSettingsSnapshot(
      userId: _readString(payload, 'user_id'),
      mobile: _readString(payload, 'mobile'),
      nickname: _readString(payload, 'nickname'),
      cityCode: _readNullableString(payload, 'city_code'),
      cityName: _readNullableString(payload, 'city_name'),
      currentPetId: _readNullableString(payload, 'current_pet_id'),
      notificationEnabled: _readBool(payload, 'notification_enabled'),
      privacyLevel: _readString(payload, 'privacy_level'),
    );
  }

  NotificationInboxSnapshot _toNotificationInboxSnapshot(
      Map<String, dynamic> payload) {
    return NotificationInboxSnapshot(
      items: _asMapList(payload['items'], context: '通知列表')
          .map(_toNotificationMessageSnapshot)
          .toList(),
      unreadCount: _readInt(payload, 'unread_count'),
      systemUnreadCount: _readInt(payload, 'system_unread_count'),
      reminderUnreadCount: _readInt(payload, 'reminder_unread_count'),
    );
  }

  NotificationMessageSnapshot _toNotificationMessageSnapshot(
      Map<String, dynamic> payload) {
    return NotificationMessageSnapshot(
      notificationId: _readString(payload, 'notification_id'),
      notifyType: _readString(payload, 'notify_type'),
      bizType: _readNullableString(payload, 'biz_type'),
      bizId: _readNullableString(payload, 'biz_id'),
      title: _readString(payload, 'title'),
      content: _readString(payload, 'content'),
      readStatus: _readNotificationReadStatus(payload),
      sentAt: _readDateTime(payload, 'sent_at'),
      readAt: _readNullableDateTime(payload, 'read_at'),
    );
  }

  String _readNotificationReadStatus(Map<String, dynamic> payload) {
    final String? readStatus = _readNullableString(payload, 'read_status');
    if (readStatus == 'read' || readStatus == 'unread') {
      return readStatus!;
    }
    return _readBool(payload, 'read') ? 'read' : 'unread';
  }

  @override
  Future<PetDashboardSnapshot> getPetDashboard(String petId) async {
    final List<dynamic> results = await Future.wait<dynamic>(<Future<dynamic>>[
      _apiClient.getData('/api/v1/pets/$petId/summary'),
      _apiClient.getData('/api/v1/pets/$petId/reminders'),
      _apiClient.getData('/api/v1/pets/$petId/health-records'),
      _apiClient.getData('/api/v1/pets/$petId/daily-logs'),
    ]);

    final Map<String, dynamic> summaryData =
        _asMap(results[0], context: '宠物摘要');
    final List<Map<String, dynamic>> reminders =
        _asMapList(results[1], context: '提醒列表');
    final List<Map<String, dynamic>> healthRecords =
        _asMapList(results[2], context: '健康记录列表');
    final List<Map<String, dynamic>> dailyLogs =
        _asMapList(results[3], context: '萌宠日常列表');

    return PetDashboardSnapshot(
      pet: _toPetProfileSnapshot(_asMap(summaryData['pet'], context: '宠物详情')),
      todayTodoCount: _readInt(summaryData, 'today_todo_count'),
      reminders: reminders.map(_toReminderSnapshot).toList(),
      healthRecords: healthRecords.map(_toHealthRecordSnapshot).toList(),
      dailyLogs: dailyLogs.map(_toDailyLogSnapshot).toList(),
    );
  }

  @override
  Future<HomeAggregateSnapshot> getHomeAggregate() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/home'),
      context: '首页聚合数据',
    );

    final Object? dashboardPayload = data['dashboard'];
    return HomeAggregateSnapshot(
      currentUser: _toCurrentUserSnapshot(
        _asMap(data['current_user'], context: '首页当前用户'),
      ),
      dashboard: dashboardPayload is Map
          ? _toHomeDashboardSnapshot(
              Map<String, dynamic>.from(dashboardPayload),
            )
          : null,
    );
  }

  @override
  Future<HomePetReportSnapshot> getWeeklyPetReport() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/home/reports/weekly'),
      context: '周报数据',
    );
    return _toHomePetReportSnapshot(data);
  }

  @override
  Future<HomePetReportSnapshot> getMonthlyPetReport() async {
    final Map<String, dynamic> data = _asMap(
      await _apiClient.getData('/api/v1/home/reports/monthly'),
      context: '月报数据',
    );
    return _toHomePetReportSnapshot(data);
  }

  PetDashboardSnapshot _toHomeDashboardSnapshot(
    Map<String, dynamic> payload,
  ) {
    return PetDashboardSnapshot(
      pet: _toPetProfileSnapshot(_asMap(payload['pet'], context: '首页当前宠物')),
      todayTodoCount: _readInt(payload, 'today_todo_count'),
      reminders: _asMapList(payload['reminders'], context: '首页提醒列表')
          .map(_toReminderSnapshot)
          .toList(),
      healthRecords: _asMapList(payload['health_records'], context: '首页健康记录列表')
          .map(_toHealthRecordSnapshot)
          .toList(),
      dailyLogs: _asMapList(payload['daily_logs'], context: '首页日常列表')
          .map(_toDailyLogSnapshot)
          .toList(),
    );
  }

  PetProfileSnapshot _toPetProfileSnapshot(Map<String, dynamic> payload) {
    return PetProfileSnapshot(
      petId: _readString(payload, 'pet_id'),
      petName: _readString(payload, 'pet_name'),
      petType: _readString(payload, 'pet_type'),
      breed: _readString(payload, 'breed'),
      gender: _readNullableString(payload, 'gender'),
    );
  }

  PetDetailSnapshot _toPetDetailSnapshot(Map<String, dynamic> payload) {
    return PetDetailSnapshot(
      petId: _readString(payload, 'pet_id'),
      petName: _readString(payload, 'pet_name'),
      petType: _readString(payload, 'pet_type'),
      breed: _readString(payload, 'breed'),
      gender: _readString(payload, 'gender'),
      neuterStatus: _readNullableString(payload, 'neuter_status') ?? 'unknown',
      birthday: _readNullableDate(payload, 'birthday'),
      adoptDate: _readNullableDate(payload, 'adopt_date'),
      avatarUrl: _readNullableString(payload, 'avatar_url'),
      weightKg: _readNullableString(payload, 'weight_kg'),
      allergyNotes: _readNullableString(payload, 'allergy_notes'),
      medicalHistory: _readNullableString(payload, 'medical_history'),
      status: _readNullableString(payload, 'status') ?? 'active',
      createdAt: _readNullableDateTime(payload, 'created_at'),
      updatedAt: _readNullableDateTime(payload, 'updated_at'),
    );
  }

  ReminderSnapshot _toReminderSnapshot(Map<String, dynamic> payload) {
    return ReminderSnapshot(
      reminderId: _readString(payload, 'reminder_id'),
      reminderType: _readString(payload, 'reminder_type'),
      title: _readString(payload, 'title'),
      reminderMode: _readString(payload, 'reminder_mode'),
      dueAt: _readDateTime(payload, 'due_at'),
      status: _readString(payload, 'status'),
      cycleValue: _readNullableInt(payload, 'cycle_value'),
      cycleUnit: _readNullableString(payload, 'cycle_unit'),
      notes: _readNullableString(payload, 'notes'),
    );
  }

  ReminderTemplateSnapshot _toReminderTemplateSnapshot(
      Map<String, dynamic> payload) {
    return ReminderTemplateSnapshot(
      templateId: _readString(payload, 'template_id'),
      templateName: _readString(payload, 'template_name'),
      reminderType: _readString(payload, 'reminder_type'),
      defaultReminderMode: _readString(payload, 'default_reminder_mode'),
      defaultAdvanceValue: _readInt(payload, 'default_advance_value'),
      defaultAdvanceUnit: _readString(payload, 'default_advance_unit'),
      defaultCycleValue: _readNullableInt(payload, 'default_cycle_value'),
      defaultCycleUnit: _readNullableString(payload, 'default_cycle_unit'),
      applicablePetType: _readString(payload, 'applicable_pet_type'),
      enabled: _readBool(payload, 'enabled'),
      sortOrder: _readInt(payload, 'sort_order'),
    );
  }

  HealthRecordSnapshot _toHealthRecordSnapshot(Map<String, dynamic> payload) {
    return HealthRecordSnapshot(
      healthRecordId: _readString(payload, 'health_record_id'),
      recordType: _readString(payload, 'record_type'),
      title: _readString(payload, 'title'),
      occurredAt: _readDateTime(payload, 'occurred_at'),
      value: _readNullableString(payload, 'value'),
      unit: _readNullableString(payload, 'unit'),
      hospitalName: _readNullableString(payload, 'hospital_name'),
      doctorName: _readNullableString(payload, 'doctor_name'),
      severityLevel: _readNullableString(payload, 'severity_level'),
      resultSummary: _readNullableString(payload, 'result_summary'),
      attachmentAssetIds:
          _asNullableStringList(payload['attachment_asset_ids']),
      nextReminderId: _readNullableString(payload, 'next_reminder_id'),
      nextReminderAt: _readNullableDateTime(payload, 'next_reminder_at'),
      nextReminderStatus: _readNullableString(payload, 'next_reminder_status'),
      notes: _readNullableString(payload, 'notes'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  DailyLogSnapshot _toDailyLogSnapshot(Map<String, dynamic> payload) {
    return DailyLogSnapshot(
      dailyLogId: _readString(payload, 'daily_log_id'),
      content: _readString(payload, 'content'),
      mediaAssetIds: _asNullableStringList(payload['media_asset_ids']),
      tags: _asStringList(payload['tags'], context: '日常标签'),
      visibility: _readString(payload, 'visibility'),
      syncToCommunity: payload['sync_to_community'] == true,
      happenedAt: _readDateTime(payload, 'happened_at'),
      communityPostId: _readNullableString(payload, 'community_post_id'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  MediaAssetSnapshot _toMediaAssetSnapshot(Map<String, dynamic> payload) {
    return MediaAssetSnapshot(
      assetId: _readString(payload, 'asset_id'),
      bizType: _readString(payload, 'biz_type'),
      mediaType: _readString(payload, 'media_type'),
      fileName: _readString(payload, 'file_name'),
      contentType: _readNullableString(payload, 'content_type'),
      fileSize: _readInt(payload, 'file_size'),
      fileHash: _readNullableString(payload, 'file_hash'),
      uploadStatus: _readString(payload, 'upload_status'),
      reviewStatus: _readString(payload, 'review_status'),
      accessUrl: _readString(payload, 'access_url'),
      completedAt: _readNullableDateTime(payload, 'completed_at'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  HomePetReportSnapshot _toHomePetReportSnapshot(Map<String, dynamic> payload) {
    return HomePetReportSnapshot(
      reportType: _readString(payload, 'report_type'),
      pet: _toPetDetailSnapshot(_asMap(payload['pet'], context: '报告宠物信息')),
      windowStart: _readDateTime(payload, 'window_start'),
      windowEnd: _readDateTime(payload, 'window_end'),
      pendingReminderCount: _readInt(payload, 'pending_reminder_count'),
      completedReminderCount: _readInt(payload, 'completed_reminder_count'),
      skippedReminderCount: _readInt(payload, 'skipped_reminder_count'),
      healthRecordCount: _readInt(payload, 'health_record_count'),
      dailyLogCount: _readInt(payload, 'daily_log_count'),
      communitySyncCount: _readInt(payload, 'community_sync_count'),
      feedCount: _readInt(payload, 'feed_count'),
      waterCount: _readInt(payload, 'water_count'),
      toiletCount: _readInt(payload, 'toilet_count'),
      weightRecordCount: _readInt(payload, 'weight_record_count'),
      medicationRecordCount: _readInt(payload, 'medication_record_count'),
      highlights: _asStringList(payload['highlights'], context: '报告亮点'),
      recentReminders:
          _asMapList(payload['recent_reminders'], context: '最近提醒列表')
              .map(_toReminderSnapshot)
              .toList(),
      recentHealthRecords: _asMapList(
        payload['recent_health_records'],
        context: '最近健康记录列表',
      ).map(_toHealthRecordSnapshot).toList(),
      recentDailyLogs:
          _asMapList(payload['recent_daily_logs'], context: '最近萌宠日常列表')
              .map(_toDailyLogSnapshot)
              .toList(),
    );
  }

  CommunityPostSnapshot _toCommunityPostSnapshot(Map<String, dynamic> payload) {
    final Map<String, dynamic> author =
        _asMap(payload['author'], context: '社区作者');
    final Object? petPayload = payload['pet'];
    final Object? topicPayload = payload['topic'];

    return CommunityPostSnapshot(
      postId: _readString(payload, 'post_id'),
      postType: _readString(payload, 'post_type'),
      title: _readString(payload, 'title'),
      content: _readString(payload, 'content'),
      sourceDailyLogId: _readNullableString(payload, 'source_daily_log_id'),
      cityCode: _readNullableString(payload, 'city_code'),
      visibility: _readString(payload, 'visibility'),
      likeCount: _readInt(payload, 'like_count'),
      commentCount: _readInt(payload, 'comment_count'),
      favoriteCount: _readInt(payload, 'favorite_count'),
      liked: _readBool(payload, 'liked'),
      favorited: _readBool(payload, 'favorited'),
      topic: topicPayload == null
          ? null
          : _toCommunityTopicSnapshot(
              _asMap(topicPayload, context: '社区话题'),
            ),
      mediaAssetIds: _asNullableStringList(payload['media_asset_ids']),
      mediaAssets: _toCommunityMediaAssets(payload['media_assets']),
      reviewStatus: _readString(payload, 'review_status'),
      publishedAt: _readNullableDateTime(payload, 'published_at'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
      author: CommunityAuthorSnapshot(
        userId: _readString(author, 'user_id'),
        nickname: _readNullableString(author, 'nickname') ?? '宠物家长',
        avatarUrl: _readNullableString(author, 'avatar_url'),
      ),
      pet: petPayload == null
          ? null
          : (() {
              final Map<String, dynamic> pet =
                  _asMap(petPayload, context: '社区关联宠物');
              return CommunityPetSnapshot(
                petId: _readString(pet, 'pet_id'),
                petName: _readString(pet, 'pet_name'),
                petType: _readString(pet, 'pet_type'),
                breed: _readNullableString(pet, 'breed'),
              );
            })(),
    );
  }

  CommunityTopicSnapshot _toCommunityTopicSnapshot(
      Map<String, dynamic> payload) {
    return CommunityTopicSnapshot(
      topicId: _readString(payload, 'topic_id'),
      topicName: _readString(payload, 'topic_name'),
      topicDesc: _readNullableString(payload, 'topic_desc'),
      cityCode: _readNullableString(payload, 'city_code'),
      status: _readNullableInt(payload, 'status'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
      updatedAt: _readNullableDateTime(payload, 'updated_at'),
    );
  }

  CommunityTopicDetailSnapshot _toCommunityTopicDetailSnapshot(
    Map<String, dynamic> payload,
  ) {
    return CommunityTopicDetailSnapshot(
      topic: _toCommunityTopicSnapshot(
        _asMap(payload['topic'], context: '社区话题信息'),
      ),
      posts: _asMapList(payload['posts'], context: '话题帖子列表')
          .map(_toCommunityPostSnapshot)
          .toList(),
    );
  }

  CommunityQuestionDetailSnapshot _toCommunityQuestionDetailSnapshot(
    Map<String, dynamic> payload,
  ) {
    return CommunityQuestionDetailSnapshot(
      question: _toCommunityPostSnapshot(
        _asMap(payload['question'], context: '问答帖子信息'),
      ),
      answers: _asMapList(payload['answers'], context: '问答回答列表')
          .map(_toCommunityCommentSnapshot)
          .toList(),
    );
  }

  CommunityFollowStatusSnapshot _toCommunityFollowStatusSnapshot(
    Map<String, dynamic> payload,
  ) {
    return CommunityFollowStatusSnapshot(
      followedUserId: _readString(payload, 'followed_user_id'),
      following: _readBool(payload, 'following'),
    );
  }

  List<MediaAssetSnapshot> _toCommunityMediaAssets(Object? value) {
    if (value == null) {
      return const <MediaAssetSnapshot>[];
    }
    return _asMapList(value, context: '社区媒体资产列表')
        .map(_toMediaAssetSnapshot)
        .toList();
  }

  CommunityCommentSnapshot _toCommunityCommentSnapshot(
      Map<String, dynamic> payload) {
    final Map<String, dynamic> author =
        _asMap(payload['author'], context: '评论作者');
    return CommunityCommentSnapshot(
      commentId: _readString(payload, 'comment_id'),
      postId: _readString(payload, 'post_id'),
      content: _readString(payload, 'content'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
      author: CommunityAuthorSnapshot(
        userId: _readString(author, 'user_id'),
        nickname: _readNullableString(author, 'nickname') ?? '宠物家长',
        avatarUrl: _readNullableString(author, 'avatar_url'),
      ),
    );
  }

  CommunityReportSnapshot _toCommunityReportSnapshot(
      Map<String, dynamic> payload) {
    return CommunityReportSnapshot(
      reportId: _readString(payload, 'report_id'),
      targetType: _readString(payload, 'target_type'),
      targetId: _readString(payload, 'target_id'),
      reasonCode: _readString(payload, 'reason_code'),
      reasonDetail: _readNullableString(payload, 'reason_detail'),
      status: _readString(payload, 'status'),
      createdAt: _readDateTime(payload, 'created_at'),
    );
  }

  ServiceHomeSnapshot _toServiceHomeSnapshot(Map<String, dynamic> payload) {
    return ServiceHomeSnapshot(
      cityCode: _readString(payload, 'city_code'),
      cityName: _readString(payload, 'city_name'),
      opened: _readBool(payload, 'opened'),
      unavailableReason: _readNullableString(payload, 'unavailable_reason'),
      categories: _asMapList(payload['categories'], context: '服务分类列表')
          .map(_toServiceCategorySnapshot)
          .toList(),
      featuredProviders:
          _asMapList(payload['featured_providers'], context: '推荐服务商列表')
              .map(_toServiceProviderSnapshot)
              .toList(),
      upcomingAppointments:
          _asMapList(payload['upcoming_appointments'], context: '近期预约列表')
              .map(_toServiceAppointmentSnapshot)
              .toList(),
      commercePlaceholder: _readString(payload, 'commerce_placeholder'),
    );
  }

  ServiceCategorySnapshot _toServiceCategorySnapshot(
      Map<String, dynamic> payload) {
    return ServiceCategorySnapshot(
      providerType: _readString(payload, 'provider_type'),
      title: _readString(payload, 'title'),
      description: _readString(payload, 'description'),
      providerCount: _readInt(payload, 'provider_count'),
      available: _readBool(payload, 'available'),
    );
  }

  ServiceProviderSnapshot _toServiceProviderSnapshot(
      Map<String, dynamic> payload) {
    return ServiceProviderSnapshot(
      providerId: _readString(payload, 'provider_id'),
      providerType: _readString(payload, 'provider_type'),
      providerName: _readString(payload, 'provider_name'),
      cityCode: _readString(payload, 'city_code'),
      address: _readNullableString(payload, 'address'),
      contactPhone: _readNullableString(payload, 'contact_phone'),
      businessHours: _readNullableString(payload, 'business_hours'),
      ratingAvg: _readNullableString(payload, 'rating_avg'),
      reviewCount: _readNullableInt(payload, 'review_count') ?? 0,
      status: _readString(payload, 'status'),
      bookable: _readBool(payload, 'bookable'),
      serviceItems: _asMapList(payload['service_items'], context: '服务项目列表')
          .map(_toProviderServiceItemSnapshot)
          .toList(),
      availableSlots: _asMapList(payload['available_slots'], context: '可预约时段列表')
          .map(_toProviderScheduleSlotSnapshot)
          .toList(),
    );
  }

  ProviderServiceItemSnapshot _toProviderServiceItemSnapshot(
      Map<String, dynamic> payload) {
    return ProviderServiceItemSnapshot(
      serviceItemId: _readString(payload, 'service_item_id'),
      serviceCode: _readString(payload, 'service_code'),
      serviceName: _readString(payload, 'service_name'),
      serviceDesc: _readNullableString(payload, 'service_desc'),
      priceMin: _readNullableString(payload, 'price_min'),
      priceMax: _readNullableString(payload, 'price_max'),
      status: _readString(payload, 'status'),
    );
  }

  ProviderScheduleSlotSnapshot _toProviderScheduleSlotSnapshot(
      Map<String, dynamic> payload) {
    return ProviderScheduleSlotSnapshot(
      slotId: _readString(payload, 'slot_id'),
      providerId: _readString(payload, 'provider_id'),
      appointmentType: _readString(payload, 'appointment_type'),
      slotDate: _readDateTime(payload, 'slot_date'),
      startTime: _readTimeLabel(payload, 'start_time'),
      endTime: _readTimeLabel(payload, 'end_time'),
      quota: _readInt(payload, 'quota'),
      bookedCount: _readInt(payload, 'booked_count'),
      availableQuota: _readInt(payload, 'available_quota'),
      status: _readString(payload, 'status'),
      bookable: _readBool(payload, 'bookable'),
    );
  }

  ServiceAppointmentSnapshot _toServiceAppointmentSnapshot(
      Map<String, dynamic> payload) {
    return ServiceAppointmentSnapshot(
      appointmentId: _readString(payload, 'appointment_id'),
      petId: _readString(payload, 'pet_id'),
      petName: _readString(payload, 'pet_name'),
      providerId: _readString(payload, 'provider_id'),
      providerName: _readString(payload, 'provider_name'),
      providerType: _readString(payload, 'provider_type'),
      appointmentType: _readString(payload, 'appointment_type'),
      appointmentDate: _readDateTime(payload, 'appointment_date'),
      appointmentSlot: _readString(payload, 'appointment_slot'),
      demandDesc: _readNullableString(payload, 'demand_desc'),
      contactName: _readString(payload, 'contact_name'),
      contactMobile: _readString(payload, 'contact_mobile'),
      status: _readString(payload, 'status'),
      reviewed: _readNullableBool(payload, 'reviewed') ?? false,
      remark: _readNullableString(payload, 'remark'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
      updatedAt: _readNullableDateTime(payload, 'updated_at'),
    );
  }

  ProviderReviewSnapshot _toProviderReviewSnapshot(
      Map<String, dynamic> payload) {
    return ProviderReviewSnapshot(
      reviewId: _readString(payload, 'review_id'),
      providerId: _readString(payload, 'provider_id'),
      providerName: _readString(payload, 'provider_name'),
      providerType: _readString(payload, 'provider_type'),
      appointmentId: _readNullableString(payload, 'appointment_id'),
      userId: _readString(payload, 'user_id'),
      reviewerNickname: _readString(payload, 'reviewer_nickname'),
      petId: _readNullableString(payload, 'pet_id'),
      petName: _readNullableString(payload, 'pet_name'),
      rating: _readInt(payload, 'rating'),
      content: _readNullableString(payload, 'content'),
      status: _readString(payload, 'status'),
      createdAt: _readDateTime(payload, 'created_at'),
      updatedAt: _readDateTime(payload, 'updated_at'),
    );
  }

  TimelineEventSnapshot _toTimelineEventSnapshot(Map<String, dynamic> payload) {
    return TimelineEventSnapshot(
      eventId: _readString(payload, 'event_id'),
      eventType: _readString(payload, 'event_type'),
      sourceType: _readString(payload, 'source_type'),
      sourceId: _readString(payload, 'source_id'),
      eventTime: _readDateTime(payload, 'event_time'),
      title: _readString(payload, 'title'),
      summary: _readNullableString(payload, 'summary'),
      coverUrl: _readNullableString(payload, 'cover_url'),
      visibility: _readString(payload, 'visibility'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  FamilyMemberSnapshot _toFamilyMemberSnapshot(Map<String, dynamic> payload) {
    return FamilyMemberSnapshot(
      memberId: _readString(payload, 'member_id'),
      userId: _readString(payload, 'user_id'),
      nickname: _readString(payload, 'nickname'),
      mobile: _readString(payload, 'mobile'),
      role: _readString(payload, 'role'),
      inviteStatus: _readString(payload, 'invite_status'),
      joinedAt: _readNullableDateTime(payload, 'joined_at'),
    );
  }

  FamilySharedPetSnapshot _toFamilySharedPetSnapshot(
      Map<String, dynamic> payload) {
    return FamilySharedPetSnapshot(
      petId: _readString(payload, 'pet_id'),
      petName: _readString(payload, 'pet_name'),
      petType: _readString(payload, 'pet_type'),
      breed: _readString(payload, 'breed'),
    );
  }

  FamilyInvitationSnapshot _toFamilyInvitationSnapshot(
      Map<String, dynamic> payload) {
    return FamilyInvitationSnapshot(
      invitationId: _readString(payload, 'invitation_id'),
      inviteeMobile: _readString(payload, 'invitee_mobile'),
      role: _readString(payload, 'role'),
      sharedPetIds:
          _asStringList(payload['shared_pet_ids'], context: '共享宠物 ID 列表'),
      inviteCode: _readString(payload, 'invite_code'),
      status: _readString(payload, 'status'),
      expiredAt: _readNullableDateTime(payload, 'expired_at'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  FamilyInvitationPreviewSnapshot _toFamilyInvitationPreviewSnapshot(
      Map<String, dynamic> payload) {
    return FamilyInvitationPreviewSnapshot(
      invitationId: _readString(payload, 'invitation_id'),
      familyId: _readString(payload, 'family_id'),
      familyName: _readString(payload, 'family_name'),
      inviterNickname: _readString(payload, 'inviter_nickname'),
      inviteeMobile: _readString(payload, 'invitee_mobile'),
      role: _readString(payload, 'role'),
      sharedPets: _asMapList(payload['shared_pets'], context: '邀请共享宠物列表')
          .map(_toFamilySharedPetSnapshot)
          .toList(),
      inviteCode: _readString(payload, 'invite_code'),
      status: _readString(payload, 'status'),
      expiredAt: _readNullableDateTime(payload, 'expired_at'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  FamilyDetailSnapshot _toFamilyDetailSnapshot(Map<String, dynamic> payload) {
    return FamilyDetailSnapshot(
      familyId: _readString(payload, 'family_id'),
      familyName: _readString(payload, 'family_name'),
      memberCount: _readInt(payload, 'member_count'),
      currentUserRole: _readString(payload, 'current_user_role'),
      members: _asMapList(payload['members'], context: '家庭成员列表')
          .map(_toFamilyMemberSnapshot)
          .toList(),
      sharedPets: _asMapList(payload['shared_pets'], context: '共享宠物列表')
          .map(_toFamilySharedPetSnapshot)
          .toList(),
      pendingInvitations:
          _asMapList(payload['pending_invitations'], context: '待处理邀请列表')
              .map(_toFamilyInvitationSnapshot)
              .toList(),
    );
  }

  Map<String, dynamic> _asMap(Object? value, {required String context}) {
    if (value is Map<String, dynamic>) {
      return value;
    }

    if (value is Map) {
      return value
          .map((Object? key, Object? item) => MapEntry(key.toString(), item));
    }

    throw ApiException('$context 结构不合法');
  }

  List<Map<String, dynamic>> _asMapList(Object? value,
      {required String context}) {
    if (value is! List) {
      throw ApiException('$context 结构不合法');
    }

    return value.map((Object? item) => _asMap(item, context: context)).toList();
  }

  List<String> _asStringList(Object? value, {required String context}) {
    if (value is! List) {
      throw ApiException('$context 结构不合法');
    }

    return value.map((Object? item) => item.toString()).toList();
  }

  List<String> _asNullableStringList(Object? value) {
    if (value == null) {
      return const <String>[];
    }
    if (value is! List) {
      return const <String>[];
    }
    return value.map((Object? item) => item.toString()).toList();
  }

  String _readString(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      throw ApiException('字段 $key 缺失');
    }

    final String result = value.toString();
    if (result.trim().isEmpty) {
      throw ApiException('字段 $key 为空');
    }

    return result;
  }

  String? _readNullableString(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      return null;
    }

    final String result = value.toString().trim();
    return result.isEmpty ? null : result;
  }

  int _readInt(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value is int) {
      return value;
    }

    final int? parsedValue = int.tryParse(value?.toString() ?? '');
    if (parsedValue == null) {
      throw ApiException('字段 $key 不是有效整数');
    }

    return parsedValue;
  }

  int? _readNullableInt(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      return null;
    }
    if (value is int) {
      return value;
    }
    return int.tryParse(value.toString());
  }

  bool _readBool(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value is bool) {
      return value;
    }
    if (value is int) {
      return value != 0;
    }
    final String normalizedValue = value?.toString().trim().toLowerCase() ?? '';
    if (normalizedValue == 'true' || normalizedValue == '1') {
      return true;
    }
    if (normalizedValue == 'false' || normalizedValue == '0') {
      return false;
    }
    throw ApiException('字段 $key 不是有效布尔值');
  }

  bool? _readNullableBool(Map<String, dynamic> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      return null;
    }
    if (value is bool) {
      return value;
    }
    if (value is int) {
      return value != 0;
    }
    final String normalizedValue = value.toString().trim().toLowerCase();
    if (normalizedValue == 'true' || normalizedValue == '1') {
      return true;
    }
    if (normalizedValue == 'false' || normalizedValue == '0') {
      return false;
    }
    return null;
  }

  DateTime _readDateTime(Map<String, dynamic> payload, String key) {
    final String value = _readString(payload, key);
    final DateTime? parsedValue = DateTime.tryParse(value);
    if (parsedValue == null) {
      throw ApiException('字段 $key 不是有效时间');
    }

    return parsedValue;
  }

  DateTime? _readNullableDateTime(Map<String, dynamic> payload, String key) {
    final String? value = _readNullableString(payload, key);
    if (value == null) {
      return null;
    }

    final DateTime? parsedValue = DateTime.tryParse(value);
    if (parsedValue == null) {
      throw ApiException('字段 $key 不是有效时间');
    }

    return parsedValue;
  }

  DateTime? _readNullableDate(Map<String, dynamic> payload, String key) {
    final String? value = _readNullableString(payload, key);
    if (value == null) {
      return null;
    }

    final DateTime? parsedValue = DateTime.tryParse(value);
    if (parsedValue == null) {
      throw ApiException('字段 $key 不是有效日期');
    }

    return parsedValue;
  }

  String _readTimeLabel(Map<String, dynamic> payload, String key) {
    final String value = _readString(payload, key);
    return value.length >= 5 ? value.substring(0, 5) : value;
  }

  Map<String, Object?> _toCommunityPostBody(CommunityPostDraft draft) {
    return <String, Object?>{
      if (draft.petId != null) 'pet_id': _toNumericId(draft.petId!),
      if (draft.topicId != null) 'topic_id': _toNumericId(draft.topicId!),
      'post_type': draft.postType,
      if (draft.title != null) 'title': draft.title,
      'content': draft.content,
      'media_asset_ids': draft.mediaAssetIds,
      if (draft.cityCode != null) 'city_code': draft.cityCode,
      'visibility': draft.visibility,
    };
  }

  Object _toNumericId(String id) {
    return int.tryParse(id) ?? id;
  }

  Map<String, Object?> _toPetUpsertBody(PetUpsertDraft draft) {
    return <String, Object?>{
      'pet_name': draft.petName,
      'pet_type': draft.petType,
      'breed': draft.breed,
      'gender': draft.gender,
      'birthday': _formatDate(draft.birthday),
      'adopt_date': _formatDate(draft.adoptDate),
      'neuter_status': draft.neuterStatus,
      'avatar_asset_id': draft.avatarAssetId,
      'weight_kg': _normalizeNullableText(draft.weightKg),
      'allergy_notes': _normalizeNullableText(draft.allergyNotes),
      'medical_history': _normalizeNullableText(draft.medicalHistory),
    };
  }

  String? _normalizeNullableText(String? value) {
    if (value == null) {
      return null;
    }

    final String trimmedValue = value.trim();
    return trimmedValue.isEmpty ? null : trimmedValue;
  }

  String? _formatDate(DateTime? value) {
    if (value == null) {
      return null;
    }

    final String month = value.month.toString().padLeft(2, '0');
    final String day = value.day.toString().padLeft(2, '0');
    return '${value.year}-$month-$day';
  }
}
