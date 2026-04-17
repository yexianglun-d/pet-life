import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/entry/presentation/pages/app_entry_page.dart';

/// 应用路由定义。
abstract final class AppRouter {
  static const String homeRoute = '/';

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case homeRoute:
        return MaterialPageRoute<void>(
          builder: (_) => const AppEntryPage(),
          settings: settings,
        );
      default:
        return MaterialPageRoute<void>(
          builder: (_) => const AppEntryPage(),
          settings: settings,
        );
    }
  }
}
