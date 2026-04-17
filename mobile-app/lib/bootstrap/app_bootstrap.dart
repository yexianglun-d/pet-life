import 'package:flutter/widgets.dart';
import 'package:petlife_mobile_app/app/pet_life_app.dart';

/// 应用启动引导。
///
/// 当前阶段仅负责完成 Flutter 绑定初始化和根应用挂载，
/// 便于后续继续接入日志、环境配置和错误上报。
abstract final class AppBootstrap {
  static void run() {
    WidgetsFlutterBinding.ensureInitialized();
    runApp(PetLifeApp());
  }
}
