import 'package:flutter/foundation.dart';

/// 应用接口地址配置。
abstract final class AppApiConfig {
  static const String _configuredBaseUrl = String.fromEnvironment(
    'PETLIFE_API_BASE_URL',
    defaultValue: 'https://pet.api.howied.me',
  );

  static Uri get baseUri {
    final String configuredBaseUrl = _configuredBaseUrl.trim();
    if (configuredBaseUrl.isNotEmpty) {
      return Uri.parse(configuredBaseUrl);
    }

    if (kIsWeb) {
      return Uri.parse('http://localhost:8080');
    }

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return Uri.parse('http://10.0.2.2:8080');
      case TargetPlatform.iOS:
      case TargetPlatform.macOS:
      case TargetPlatform.windows:
      case TargetPlatform.linux:
      case TargetPlatform.fuchsia:
        return Uri.parse('http://127.0.0.1:8080');
    }
  }
}
