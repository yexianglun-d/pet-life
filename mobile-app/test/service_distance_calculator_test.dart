import 'package:flutter_test/flutter_test.dart';
import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';
import 'package:petlife_mobile_app/shared/location/petlife_location_models.dart';
import 'package:petlife_mobile_app/shared/location/service_distance_calculator.dart';

void main() {
  test('uses server distance before local coordinate calculation', () {
    final ServiceProviderSnapshot provider = _provider(
      latitude: 31.2304,
      longitude: 121.4737,
      distanceMeters: 860,
    );

    final int? distance = ServiceDistanceCalculator.resolveDistanceMeters(
      provider: provider,
      currentCoordinate: const PetLifeCoordinate(
        latitude: 30.0,
        longitude: 120.0,
      ),
    );

    expect(distance, 860);
  });

  test('calculates local distance when provider has coordinates', () {
    final ServiceProviderSnapshot provider = _provider(
      latitude: 31.2304,
      longitude: 121.4737,
    );

    final int? distance = ServiceDistanceCalculator.resolveDistanceMeters(
      provider: provider,
      currentCoordinate: const PetLifeCoordinate(
        latitude: 31.2310,
        longitude: 121.4740,
      ),
    );

    expect(distance, isNotNull);
    expect(distance!, greaterThan(0));
    expect(distance, lessThan(200));
  });

  test('keeps providers without coordinates out of distance sorting', () {
    final int? distance = ServiceDistanceCalculator.resolveDistanceMeters(
      provider: _provider(),
      currentCoordinate: const PetLifeCoordinate(
        latitude: 31.2310,
        longitude: 121.4740,
      ),
    );

    expect(distance, isNull);
  });
}

ServiceProviderSnapshot _provider({
  double? latitude,
  double? longitude,
  int? distanceMeters,
}) {
  return ServiceProviderSnapshot(
    providerId: '1',
    providerType: 'hospital',
    providerName: '安心宠物医院',
    cityCode: '310000',
    status: 'online',
    bookable: true,
    serviceItems: const <ProviderServiceItemSnapshot>[],
    availableSlots: const <ProviderScheduleSlotSnapshot>[],
    latitude: latitude,
    longitude: longitude,
    distanceMeters: distanceMeters,
  );
}
