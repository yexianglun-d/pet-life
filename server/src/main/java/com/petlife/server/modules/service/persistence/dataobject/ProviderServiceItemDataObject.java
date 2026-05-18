package com.petlife.server.modules.service.persistence.dataobject;

import java.math.BigDecimal;

/**
 * 服务项目持久化读模型。
 */
public record ProviderServiceItemDataObject(
    Long serviceItemId,
    Long providerId,
    String serviceCode,
    String serviceName,
    String serviceDesc,
    BigDecimal priceMin,
    BigDecimal priceMax,
    String status
) {
}
