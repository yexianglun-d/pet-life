package com.petlife.server.modules.service.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 服务城市开通配置持久化读模型。
 */
public record ServiceCityConfigDataObject(
    Long configId,
    String cityCode,
    String cityName,
    Boolean opened,
    String unavailableReason,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
