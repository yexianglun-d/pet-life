import 'dart:async';

import 'package:geolocator/geolocator.dart';
import 'package:petlife_mobile_app/shared/location/petlife_location_models.dart';

class PetLifeLocationService {
  const PetLifeLocationService();

  Future<PetLifeLocationResult> requestCurrentLocation({
    Duration timeout = const Duration(seconds: 15),
  }) async {
    final bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.serviceDisabled,
        message: '系统定位服务未开启',
      );
    }

    final LocationPermission permission = await _requestPermission();
    if (permission == LocationPermission.deniedForever) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.permanentlyDenied,
        message: '定位权限已关闭',
      );
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.unableToDetermine) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.denied,
        message: '定位权限未开启',
      );
    }

    try {
      final Position position = await Geolocator.getCurrentPosition(
        locationSettings: LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: timeout,
        ),
      );
      return PetLifeLocationResult(
        status: PetLifeLocationStatus.ready,
        message: '已获取当前位置',
        coordinate: PetLifeCoordinate(
          latitude: position.latitude,
          longitude: position.longitude,
        ),
      );
    } on TimeoutException {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.failed,
        message: '定位超时',
      );
    } on LocationServiceDisabledException {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.serviceDisabled,
        message: '系统定位服务未开启',
      );
    } catch (error) {
      return PetLifeLocationResult(
        status: PetLifeLocationStatus.failed,
        message: '定位失败：$error',
      );
    }
  }

  Future<LocationPermission> _requestPermission() async {
    final LocationPermission currentPermission =
        await Geolocator.checkPermission();
    if (currentPermission == LocationPermission.whileInUse ||
        currentPermission == LocationPermission.always ||
        currentPermission == LocationPermission.deniedForever) {
      return currentPermission;
    }
    return Geolocator.requestPermission();
  }
}
