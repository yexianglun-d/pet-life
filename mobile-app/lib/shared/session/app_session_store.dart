import 'package:shared_preferences/shared_preferences.dart';

/// 应用会话存储。
///
/// 当前阶段只持久化访问令牌，目的是先把登录态闭环打通。
class AppSessionStore {
  static const String _accessTokenKey = 'petlife_access_token';

  SharedPreferences? _sharedPreferences;

  Future<bool> hasAccessToken() async {
    return (await readAccessToken()) != null;
  }

  Future<String?> readAccessToken() async {
    final SharedPreferences preferences = await _preferences;
    final String? accessToken = preferences.getString(_accessTokenKey);
    if (accessToken == null || accessToken.trim().isEmpty) {
      return null;
    }

    return accessToken;
  }

  Future<void> saveAccessToken(String accessToken) async {
    final SharedPreferences preferences = await _preferences;
    await preferences.setString(_accessTokenKey, accessToken);
  }

  Future<void> clear() async {
    final SharedPreferences preferences = await _preferences;
    await preferences.remove(_accessTokenKey);
  }

  Future<SharedPreferences> get _preferences async {
    return _sharedPreferences ??= await SharedPreferences.getInstance();
  }
}
