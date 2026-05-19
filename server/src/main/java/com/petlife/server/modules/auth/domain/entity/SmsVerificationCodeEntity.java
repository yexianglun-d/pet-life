package com.petlife.server.modules.auth.domain.entity;

import java.time.LocalDateTime;

/**
 * 短信验证码领域实体。
 */
public final class SmsVerificationCodeEntity {

    private final Long verificationId;
    private final String mobile;
    private final String scene;
    private final LocalDateTime expiresAt;
    private final LocalDateTime verifiedAt;
    private final Integer attemptCount;
    private final Integer maxAttemptCount;
    private final String status;
    private final String requestIp;
    private final String userAgent;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SmsVerificationCodeEntity(
        Long verificationId,
        String mobile,
        String scene,
        LocalDateTime expiresAt,
        LocalDateTime verifiedAt,
        Integer attemptCount,
        Integer maxAttemptCount,
        String status,
        String requestIp,
        String userAgent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.verificationId = verificationId;
        this.mobile = mobile;
        this.scene = scene;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.attemptCount = attemptCount;
        this.maxAttemptCount = maxAttemptCount;
        this.status = status;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public String getMobile() {
        return mobile;
    }

    public String getScene() {
        return scene;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public Integer getMaxAttemptCount() {
        return maxAttemptCount;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
