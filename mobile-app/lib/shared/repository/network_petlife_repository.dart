import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_profile_snapshot.dart';
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

  ReminderSnapshot _toReminderSnapshot(Map<String, dynamic> payload) {
    return ReminderSnapshot(
      reminderId: _readString(payload, 'reminder_id'),
      reminderType: _readString(payload, 'reminder_type'),
      title: _readString(payload, 'title'),
      dueAt: _readDateTime(payload, 'due_at'),
      status: _readString(payload, 'status'),
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
    );
  }

  DailyLogSnapshot _toDailyLogSnapshot(Map<String, dynamic> payload) {
    return DailyLogSnapshot(
      dailyLogId: _readString(payload, 'daily_log_id'),
      content: _readString(payload, 'content'),
      tags: _asStringList(payload['tags'], context: '日常标签'),
      visibility: _readString(payload, 'visibility'),
      happenedAt: _readDateTime(payload, 'happened_at'),
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

  DateTime _readDateTime(Map<String, dynamic> payload, String key) {
    final String value = _readString(payload, key);
    final DateTime? parsedValue = DateTime.tryParse(value);
    if (parsedValue == null) {
      throw ApiException('字段 $key 不是有效时间');
    }

    return parsedValue;
  }
}
