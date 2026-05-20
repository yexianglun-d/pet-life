enum PetLifeLocationStatus {
  notRequested,
  locating,
  ready,
  serviceDisabled,
  denied,
  permanentlyDenied,
  keyMissing,
  failed,
}

/// 统一承载移动端定位结果，避免页面直接依赖高德 SDK 返回的松散 Map。
class PetLifeCoordinate {
  const PetLifeCoordinate({
    required this.latitude,
    required this.longitude,
  });

  final double latitude;
  final double longitude;
}

class PetLifeLocationResult {
  const PetLifeLocationResult({
    required this.status,
    required this.message,
    this.coordinate,
    this.address,
    this.cityName,
    this.cityCode,
  });

  const PetLifeLocationResult.notRequested()
      : status = PetLifeLocationStatus.notRequested,
        message = '可以使用当前位置计算距离，也可以直接浏览服务商。',
        coordinate = null,
        address = null,
        cityName = null,
        cityCode = null;

  const PetLifeLocationResult.locating()
      : status = PetLifeLocationStatus.locating,
        message = '正在确认你和服务商之间的距离。',
        coordinate = null,
        address = null,
        cityName = null,
        cityCode = null;

  final PetLifeLocationStatus status;
  final String message;
  final PetLifeCoordinate? coordinate;
  final String? address;
  final String? cityName;
  final String? cityCode;

  bool get hasCoordinate => coordinate != null;

  bool get canRetry =>
      status == PetLifeLocationStatus.notRequested ||
      status == PetLifeLocationStatus.denied ||
      status == PetLifeLocationStatus.serviceDisabled ||
      status == PetLifeLocationStatus.failed;
}
