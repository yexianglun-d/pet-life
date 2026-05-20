package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * 用户 Push 设备 Token 实体。
 */
public final class PushDeviceTokenEntity {

    private final Long deviceTokenId;
    private final Long userId;
    private final String platform;
    private final String providerCode;
    private final String deviceTokenSuffix;
    private final String deviceId;
    private final String appVersion;
    private final Boolean enabled;
    private final LocalDateTime lastRegisteredAt;
    private final LocalDateTime unregisteredAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PushDeviceTokenEntity(
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
        this.deviceTokenId = deviceTokenId;
        this.userId = userId;
        this.platform = platform;
        this.providerCode = providerCode;
        this.deviceTokenSuffix = deviceTokenSuffix;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.enabled = enabled;
        this.lastRegisteredAt = lastRegisteredAt;
        this.unregisteredAt = unregisteredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getDeviceTokenId() {
        return deviceTokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getDeviceTokenSuffix() {
        return deviceTokenSuffix;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getLastRegisteredAt() {
        return lastRegisteredAt;
    }

    public LocalDateTime getUnregisteredAt() {
        return unregisteredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
