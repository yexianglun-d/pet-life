import 'package:flutter/widgets.dart';
import 'package:petlife_mobile_app/shared/repository/petlife_repository.dart';
import 'package:petlife_mobile_app/shared/session/app_session_store.dart';

/// 应用级依赖注入容器。
class PetLifeAppScope extends InheritedWidget {
  const PetLifeAppScope({
    super.key,
    required this.repository,
    required this.sessionStore,
    required super.child,
  });

  final PetLifeRepository repository;
  final AppSessionStore sessionStore;

  static PetLifeRepository repositoryOf(BuildContext context) {
    final PetLifeAppScope? scope =
        context.dependOnInheritedWidgetOfExactType<PetLifeAppScope>();
    assert(scope != null, 'PetLifeAppScope 未注入');
    return scope!.repository;
  }

  static AppSessionStore sessionStoreOf(BuildContext context) {
    final PetLifeAppScope? scope =
        context.dependOnInheritedWidgetOfExactType<PetLifeAppScope>();
    assert(scope != null, 'PetLifeAppScope 未注入');
    return scope!.sessionStore;
  }

  @override
  bool updateShouldNotify(PetLifeAppScope oldWidget) {
    return repository != oldWidget.repository ||
        sessionStore != oldWidget.sessionStore;
  }
}
