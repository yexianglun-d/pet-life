package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * Push 设备 Token 数据对象。
 */
public record PushDeviceTokenDataObject(
    Long deviceTokenId,
    Long userId,
    String platform,
    String providerCode,
    String deviceTokenSuffix,
    String deviceId,
    String appVersion,
    Boolean enabled,
    LocalDateTime lastRegisteredAt,
    LocalDateTime unregisteredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
