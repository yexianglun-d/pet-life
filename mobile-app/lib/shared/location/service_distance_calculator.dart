import 'dart:math' as math;

import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';
import 'package:petlife_mobile_app/shared/location/petlife_location_models.dart';

abstract final class ServiceDistanceCalculator {
  static int? resolveDistanceMeters({
    required ServiceProviderSnapshot provider,
    PetLifeCoordinate? currentCoordinate,
  }) {
    if (provider.distanceMeters != null) {
      return provider.distanceMeters;
    }
    if (currentCoordinate == null || !provider.hasCoordinate) {
      return null;
    }
    return _haversineMeters(
      currentCoordinate.latitude,
      currentCoordinate.longitude,
      provider.latitude!,
      provider.longitude!,
    );
  }

  static String formatDistance(int meters) {
    if (meters < 1000) {
      return '$meters 米';
    }
    final double kilometers = meters / 1000;
    final int fractionDigits = kilometers < 10 ? 1 : 0;
    return '${kilometers.toStringAsFixed(fractionDigits)} 公里';
  }

  static int _haversineMeters(
    double startLatitude,
    double startLongitude,
    double endLatitude,
    double endLongitude,
  ) {
    const double earthRadiusMeters = 6371000;
    final double startLatRad = _toRadians(startLatitude);
    final double endLatRad = _toRadians(endLatitude);
    final double latDelta = _toRadians(endLatitude - startLatitude);
    final double lonDelta = _toRadians(endLongitude - startLongitude);

    final double a = math.sin(latDelta / 2) * math.sin(latDelta / 2) +
        math.cos(startLatRad) *
            math.cos(endLatRad) *
            math.sin(lonDelta / 2) *
            math.sin(lonDelta / 2);
    final double c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
    return (earthRadiusMeters * c).round();
  }

  static double _toRadians(double degrees) => degrees * math.pi / 180;
}
