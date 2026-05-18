import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/auth/presentation/pages/login_page.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/shell/presentation/pages/app_shell_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 应用入口页。
class AppEntryPage extends StatefulWidget {
  const AppEntryPage({super.key});

  @override
  State<AppEntryPage> createState() => _AppEntryPageState();
}

class _AppEntryPageState extends State<AppEntryPage> {
  Future<bool>? _sessionFuture;
  AppSessionStore? _sessionStore;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final sessionStore = PetLifeAppScope.sessionStoreOf(context);
    if (!identical(_sessionStore, sessionStore)) {
      _sessionStore?.removeListener(_handleSessionChanged);
      _sessionStore = sessionStore;
      _sessionStore?.addListener(_handleSessionChanged);
    }
    _sessionFuture ??= _checkSession();
  }

  @override
  void dispose() {
    _sessionStore?.removeListener(_handleSessionChanged);
    super.dispose();
  }

  Future<bool> _checkSession() async {
    final repository = PetLifeAppScope.repositoryOf(context);
    return repository.hasLocalSession();
  }

  void _refreshSession() {
    setState(() {
      _sessionFuture = _checkSession();
    });
  }

  void _handleSessionChanged() {
    if (!mounted) {
      return;
    }
    _refreshSession();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _sessionFuture,
      builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Scaffold(
            backgroundColor: AppThemePalette.background,
            body: CompanionPageLoading(
              title: '正在回到宠物生活空间',
              description: '我们在确认本地登录状态，稍后会进入你的陪伴首页。',
              icon: Icons.home_rounded,
              layout: CompanionLoadingLayout.compact,
            ),
          );
        }

        if (snapshot.data ?? false) {
          return AppShellPage(onLogoutCompleted: _refreshSession);
        }

        return LoginPage(onLoginSuccess: _refreshSession);
      },
    );
  }
}
