package com.petlife.server.modules.service.domain.entity;

import java.time.LocalDateTime;

/**
 * 服务城市开通配置实体。
 *
 * <p>城市是否开放给用户端必须由该配置显式决定，不能由服务商数量隐式推断。</p>
 */
public class ServiceCityConfigEntity {

    private final Long configId;
    private final String cityCode;
    private final String cityName;
    private final boolean opened;
    private final String unavailableReason;
    private final Integer sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ServiceCityConfigEntity(
        Long configId,
        String cityCode,
        String cityName,
        boolean opened,
        String unavailableReason,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.configId = configId;
        this.cityCode = cityCode;
        this.cityName = cityName;
        this.opened = opened;
        this.unavailableReason = unavailableReason;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getConfigId() {
        return configId;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public boolean isOpened() {
        return opened;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
