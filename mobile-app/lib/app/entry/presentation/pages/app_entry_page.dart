import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/auth/presentation/pages/login_page.dart';
import 'package:petlife_mobile_app/modules/shell/presentation/pages/app_shell_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';

/// 应用入口页。
class AppEntryPage extends StatefulWidget {
  const AppEntryPage({super.key});

  @override
  State<AppEntryPage> createState() => _AppEntryPageState();
}

class _AppEntryPageState extends State<AppEntryPage> {
  Future<bool>? _sessionFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _sessionFuture ??= _checkSession();
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

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _sessionFuture,
      builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Scaffold(
            body: Center(
              child: CircularProgressIndicator(),
            ),
          );
        }

        if (snapshot.data ?? false) {
          return const AppShellPage();
        }

        return LoginPage(onLoginSuccess: _refreshSession);
      },
    );
  }
}
