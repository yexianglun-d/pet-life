package com.petlife.server.modules.location.domain.entity;

/**
 * 高德逆地理编码结果。
 */
public record AmapReverseGeocodeEntity(
    String formattedAddress,
    String country,
    String province,
    String city,
    String district,
    String township,
    String cityCode,
    String adcode,
    GeoPointEntity point
) {
}
