package com.petlife.server.modules.notification.dto.response;

import java.time.OffsetDateTime;

/**
 * Push 设备 Token 响应。
 */
public record PushDeviceTokenResponse(
    String deviceTokenId,
    String userId,
    String platform,
    String providerCode,
    String deviceTokenSuffix,
    String deviceId,
    String appVersion,
    Boolean enabled,
    OffsetDateTime lastRegisteredAt,
    OffsetDateTime unregisteredAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
