package com.petlife.server.modules.location.dto.response;

import java.math.BigDecimal;

/**
 * 逆地理编码响应。
 */
public record AmapReverseGeocodeResponse(
    boolean matched,
    String formattedAddress,
    String country,
    String province,
    String city,
    String district,
    String township,
    String cityCode,
    String adcode,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
