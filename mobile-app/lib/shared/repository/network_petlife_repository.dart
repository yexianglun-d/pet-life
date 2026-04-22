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
    return _sessionStore.hasAccessToken();
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

    await _sessionStore.saveAccessToken(_readString(data, 'access_token'));
  }

  @override
  Future<void> logout() {
    return _sessionStore.clear();
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
  Future<List<PetDetailSnapshot>> listPets() async {
    final List<Map<String, dynamic>> pets = _asMapList(
      await _apiClient.getData('/api/v1/pets'),
      context: '宠物列表',
    );

    return pets.map(_toPetDetailSnapshot).toList();
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
          'tags': draft.tags,
          'visibility': draft.visibility,
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
          'tags': draft.tags,
          'visibility': draft.visibility,
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
    final Map<String, dynamic> currentPet =
        _asMap(data['current_pet'], context: '当前宠物信息');
    final Map<String, dynamic> familySummary =
        _asMap(data['family_summary'], context: '家庭信息');

    return CurrentUserSnapshot(
      userId: _readString(user, 'user_id'),
      nickname: _readString(user, 'nickname'),
      familyName: _readString(familySummary, 'family_name'),
      currentPetId: _readString(data, 'current_pet_id'),
      currentPet: _toPetProfileSnapshot(currentPet),
    );
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

  HealthRecordSnapshot _toHealthRecordSnapshot(Map<String, dynamic> payload) {
    return HealthRecordSnapshot(
      healthRecordId: _readString(payload, 'health_record_id'),
      recordType: _readString(payload, 'record_type'),
      title: _readString(payload, 'title'),
      occurredAt: _readDateTime(payload, 'occurred_at'),
      value: _readNullableString(payload, 'value'),
      unit: _readNullableString(payload, 'unit'),
      notes: _readNullableString(payload, 'notes'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
    );
  }

  DailyLogSnapshot _toDailyLogSnapshot(Map<String, dynamic> payload) {
    return DailyLogSnapshot(
      dailyLogId: _readString(payload, 'daily_log_id'),
      content: _readString(payload, 'content'),
      tags: _asStringList(payload['tags'], context: '日常标签'),
      visibility: _readString(payload, 'visibility'),
      happenedAt: _readDateTime(payload, 'happened_at'),
      createdAt: _readNullableDateTime(payload, 'created_at'),
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
    };
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
