package com.petlife.server.modules.location.domain.entity;

/**
 * 高德地理编码结果。
 */
public record AmapGeocodeEntity(
    String formattedAddress,
    String country,
    String province,
    String city,
    String district,
    String cityCode,
    String adcode,
    GeoPointEntity point,
    String level
) {
}
