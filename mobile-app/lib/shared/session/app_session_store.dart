import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 应用会话存储。
///
/// 当前阶段同时持久化 access token 与 refresh token，并在令牌变化时通知应用入口刷新分流。
class AppSessionStore extends ChangeNotifier {
  static const String _accessTokenKey = 'petlife_access_token';
  static const String _refreshTokenKey = 'petlife_refresh_token';

  SharedPreferences? _sharedPreferences;

  Future<bool> hasSession() async {
    return (await readAccessToken()) != null ||
        (await readRefreshToken()) != null;
  }

  Future<String?> readAccessToken() async {
    final SharedPreferences preferences = await _preferences;
    final String? accessToken = preferences.getString(_accessTokenKey);
    if (accessToken == null || accessToken.trim().isEmpty) {
      return null;
    }

    return accessToken;
  }

  Future<String?> readRefreshToken() async {
    final SharedPreferences preferences = await _preferences;
    final String? refreshToken = preferences.getString(_refreshTokenKey);
    if (refreshToken == null || refreshToken.trim().isEmpty) {
      return null;
    }
    return refreshToken;
  }

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
  }) async {
    final SharedPreferences preferences = await _preferences;
    await preferences.setString(_accessTokenKey, accessToken);
    await preferences.setString(_refreshTokenKey, refreshToken);
    notifyListeners();
  }

  Future<void> clear() async {
    final SharedPreferences preferences = await _preferences;
    final bool hadSession = preferences.containsKey(_accessTokenKey) ||
        preferences.containsKey(_refreshTokenKey);
    await preferences.remove(_accessTokenKey);
    await preferences.remove(_refreshTokenKey);
    if (hadSession) {
      notifyListeners();
    }
  }

  Future<SharedPreferences> get _preferences async {
    return _sharedPreferences ??= await SharedPreferences.getInstance();
  }
}
