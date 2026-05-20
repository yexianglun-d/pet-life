package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * Push 投递记录实体。
 */
public final class PushDeliveryRecordEntity {

    private final Long deliveryRecordId;
    private final Long pushTaskId;
    private final Long deviceTokenId;
    private final Long userId;
    private final String providerCode;
    private final String deliveryStatus;
    private final String failureReason;
    private final LocalDateTime attemptedAt;
    private final LocalDateTime createdAt;

    public PushDeliveryRecordEntity(
        Long deliveryRecordId,
        Long pushTaskId,
        Long deviceTokenId,
        Long userId,
        String providerCode,
        String deliveryStatus,
        String failureReason,
        LocalDateTime attemptedAt,
        LocalDateTime createdAt
    ) {
        this.deliveryRecordId = deliveryRecordId;
        this.pushTaskId = pushTaskId;
        this.deviceTokenId = deviceTokenId;
        this.userId = userId;
        this.providerCode = providerCode;
        this.deliveryStatus = deliveryStatus;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
        this.createdAt = createdAt;
    }

    public Long getDeliveryRecordId() {
        return deliveryRecordId;
    }

    public Long getPushTaskId() {
        return pushTaskId;
    }

    public Long getDeviceTokenId() {
        return deviceTokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
