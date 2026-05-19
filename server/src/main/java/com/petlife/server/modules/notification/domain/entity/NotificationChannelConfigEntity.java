package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * 通知发送渠道配置领域实体。
 */
public final class NotificationChannelConfigEntity {

    private final Long channelConfigId;
    private final String channelType;
    private final String providerCode;
    private final String providerName;
    private final boolean enabled;
    private final String configStatus;
    private final String remark;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public NotificationChannelConfigEntity(
        Long channelConfigId,
        String channelType,
        String providerCode,
        String providerName,
        boolean enabled,
        String configStatus,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.channelConfigId = channelConfigId;
        this.channelType = channelType;
        this.providerCode = providerCode;
        this.providerName = providerName;
        this.enabled = enabled;
        this.configStatus = configStatus;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getChannelConfigId() {
        return channelConfigId;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConfigStatus() {
        return configStatus;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
