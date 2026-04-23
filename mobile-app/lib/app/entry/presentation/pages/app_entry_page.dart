import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/auth/presentation/pages/login_page.dart';
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
          return Scaffold(
            backgroundColor: AppThemePalette.background,
            body: Center(
              child: Container(
                width: 240,
                padding: const EdgeInsets.all(28),
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(32),
                  border: Border.all(color: AppThemePalette.line),
                  boxShadow: AppThemePalette.softShadow,
                ),
                child: const Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(),
                    SizedBox(height: 18),
                    Text('正在回到毛孩子的生活空间'),
                  ],
                ),
              ),
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
