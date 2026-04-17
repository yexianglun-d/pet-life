import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 用户端仓储抽象。
abstract interface class PetLifeRepository {
  Future<bool> hasLocalSession();

  Future<void> loginBySms({
    required String mobile,
    required String code,
  });

  Future<void> logout();

  Future<CurrentUserSnapshot> getCurrentUser();

  Future<PetDashboardSnapshot> getPetDashboard(String petId);
}
