import 'dart:async';

import 'package:csp_amap_flutter_location/amap_flutter_location.dart';
import 'package:csp_amap_flutter_location/amap_location_option.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:petlife_mobile_app/shared/location/petlife_location_models.dart';

class PetLifeLocationService {
  const PetLifeLocationService();

  static const String _androidKey = String.fromEnvironment('AMAP_ANDROID_KEY');
  static const String _iosKey = String.fromEnvironment('AMAP_IOS_KEY');

  Future<PetLifeLocationResult> requestCurrentLocation({
    Duration timeout = const Duration(seconds: 15),
  }) async {
    final ServiceStatus serviceStatus =
        await Permission.locationWhenInUse.serviceStatus;
    if (serviceStatus == ServiceStatus.disabled) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.serviceDisabled,
        message: '系统定位服务暂未开启，开启后可以计算附近距离。',
      );
    }

    final PermissionStatus permissionStatus = await _requestPermission();
    if (permissionStatus.isPermanentlyDenied) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.permanentlyDenied,
        message: '定位权限已关闭，需要到系统设置里开启后再计算距离。',
      );
    }
    if (!permissionStatus.isGranted) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.denied,
        message: '未获得定位权限，仍可继续浏览服务商列表。',
      );
    }

    AMapFlutterLocation.updatePrivacyShow(true, true);
    AMapFlutterLocation.updatePrivacyAgree(true);
    if (_androidKey.trim().isNotEmpty || _iosKey.trim().isNotEmpty) {
      AMapFlutterLocation.setApiKey(_androidKey, _iosKey);
    }

    final AMapFlutterLocation locationPlugin = AMapFlutterLocation();
    StreamSubscription<Map<String, Object>>? subscription;
    final Completer<PetLifeLocationResult> completer =
        Completer<PetLifeLocationResult>();

    try {
      subscription = locationPlugin.onLocationChanged().listen(
        (Map<String, Object> result) {
          if (completer.isCompleted) {
            return;
          }
          completer.complete(_toLocationResult(result));
        },
        onError: (Object error) {
          if (completer.isCompleted) {
            return;
          }
          completer.complete(PetLifeLocationResult(
            status: PetLifeLocationStatus.failed,
            message: '定位暂时失败：$error',
          ));
        },
      );

      locationPlugin.setLocationOption(AMapLocationOption(
        onceLocation: true,
        needAddress: true,
        geoLanguage: GeoLanguage.ZH,
        locationMode: AMapLocationMode.Hight_Accuracy,
        desiredAccuracy: DesiredAccuracy.Best,
      ));
      locationPlugin.startLocation();

      return await completer.future.timeout(
        timeout,
        onTimeout: () => const PetLifeLocationResult(
          status: PetLifeLocationStatus.failed,
          message: '定位等待时间较长，可以稍后重试或直接浏览服务商。',
        ),
      );
    } finally {
      await subscription?.cancel();
      locationPlugin.stopLocation();
      locationPlugin.destroy();
    }
  }

  Future<PermissionStatus> _requestPermission() async {
    final PermissionStatus currentStatus =
        await Permission.locationWhenInUse.status;
    if (currentStatus.isGranted || currentStatus.isPermanentlyDenied) {
      return currentStatus;
    }
    return Permission.locationWhenInUse.request();
  }

  PetLifeLocationResult _toLocationResult(Map<String, Object> payload) {
    final String? errorCode = _readNullableString(payload, 'errorCode');
    if (errorCode != null && errorCode != '0') {
      final String errorInfo =
          _readNullableString(payload, 'errorInfo') ?? '请稍后重试';
      return PetLifeLocationResult(
        status: PetLifeLocationStatus.failed,
        message: '定位暂时失败：$errorInfo',
      );
    }

    final double? latitude = _readNullableDouble(payload, 'latitude');
    final double? longitude = _readNullableDouble(payload, 'longitude');
    if (latitude == null || longitude == null) {
      return const PetLifeLocationResult(
        status: PetLifeLocationStatus.failed,
        message: '这次没有拿到有效坐标，可以稍后重试。',
      );
    }

    return PetLifeLocationResult(
      status: PetLifeLocationStatus.ready,
      message: '已获取当前位置，可以按距离查看附近服务商。',
      coordinate: PetLifeCoordinate(latitude: latitude, longitude: longitude),
      address: _readNullableString(payload, 'address'),
      cityName: _readNullableString(payload, 'city'),
      cityCode: _readNullableString(payload, 'cityCode'),
    );
  }

  String? _readNullableString(Map<String, Object> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      return null;
    }
    final String result = value.toString().trim();
    return result.isEmpty ? null : result;
  }

  double? _readNullableDouble(Map<String, Object> payload, String key) {
    final Object? value = payload[key];
    if (value == null) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value.toString());
  }
}
