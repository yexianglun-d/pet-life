import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:petlife_mobile_app/app/router/app_router.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/config/app_api_config.dart';
import 'package:petlife_mobile_app/shared/network/api_client.dart';
import 'package:petlife_mobile_app/shared/repository/network_petlife_repository.dart';
import 'package:petlife_mobile_app/shared/repository/petlife_repository.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 用户端根应用。
class PetLifeApp extends StatelessWidget {
  PetLifeApp({
    Key? key,
    PetLifeRepository? repository,
    AppSessionStore? sessionStore,
  }) : this._internal(
          key: key,
          repository: repository,
          sessionStore: sessionStore ?? AppSessionStore(),
        );

  PetLifeApp._internal({
    super.key,
    PetLifeRepository? repository,
    required AppSessionStore sessionStore,
  })  : _sessionStore = sessionStore,
        _repository = repository ??
            NetworkPetLifeRepository(
              apiClient: ApiClient(
                baseUri: AppApiConfig.baseUri,
                sessionStore: sessionStore,
              ),
              sessionStore: sessionStore,
            );

  final PetLifeRepository _repository;
  final AppSessionStore _sessionStore;

  @override
  Widget build(BuildContext context) {
    return PetLifeAppScope(
      repository: _repository,
      sessionStore: _sessionStore,
      child: MaterialApp(
        title: '宠物生活管家',
        debugShowCheckedModeBanner: false,
        locale:
            const Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hans'),
        supportedLocales: const <Locale>[
          Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hans'),
          Locale('en'),
        ],
        localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
          GlobalMaterialLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
        ],
        theme: AppTheme.lightTheme,
        onGenerateRoute: AppRouter.onGenerateRoute,
        initialRoute: AppRouter.homeRoute,
      ),
    );
  }
}
