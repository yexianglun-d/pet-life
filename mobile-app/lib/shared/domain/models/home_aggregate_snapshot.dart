import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 首页聚合快照。
///
/// 服务端已经提供 `/api/v1/home` 聚合接口，移动端首页入口只消费这一份读模型，
/// 避免启动首页时继续串联多条业务接口造成状态不一致。
class HomeAggregateSnapshot {
  const HomeAggregateSnapshot({
    required this.currentUser,
    required this.dashboard,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot? dashboard;
}
