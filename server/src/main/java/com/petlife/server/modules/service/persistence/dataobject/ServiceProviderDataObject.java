package com.petlife.server.modules.service.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务商持久化读模型。
 */
public record ServiceProviderDataObject(
    Long providerId,
    String providerType,
    String providerName,
    String cityCode,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String coordinateSource,
    String contactPhone,
    String businessHours,
    BigDecimal ratingAvg,
    Integer reviewCount,
    String status,
    String extJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
