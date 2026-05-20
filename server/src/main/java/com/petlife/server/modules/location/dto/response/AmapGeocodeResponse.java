package com.petlife.server.modules.location.dto.response;

import java.math.BigDecimal;

/**
 * 地理编码响应。
 */
public record AmapGeocodeResponse(
    boolean matched,
    String address,
    String formattedAddress,
    String country,
    String province,
    String city,
    String district,
    String cityCode,
    String adcode,
    BigDecimal latitude,
    BigDecimal longitude,
    String level
) {
}
