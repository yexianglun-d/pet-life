package com.petlife.server.modules.auth.domain.entity;

import java.time.LocalDateTime;

/**
 * 短信发送记录领域实体。
 */
public final class SmsSendRecordEntity {

    private final Long sendRecordId;
    private final Long verificationId;
    private final String mobile;
    private final String scene;
    private final String providerCode;
    private final String sendStatus;
    private final String failureReason;
    private final String requestIp;
    private final String userAgent;
    private final LocalDateTime createdAt;

    public SmsSendRecordEntity(
        Long sendRecordId,
        Long verificationId,
        String mobile,
        String scene,
        String providerCode,
        String sendStatus,
        String failureReason,
        String requestIp,
        String userAgent,
        LocalDateTime createdAt
    ) {
        this.sendRecordId = sendRecordId;
        this.verificationId = verificationId;
        this.mobile = mobile;
        this.scene = scene;
        this.providerCode = providerCode;
        this.sendStatus = sendStatus;
        this.failureReason = failureReason;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public Long getSendRecordId() {
        return sendRecordId;
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

    public String getProviderCode() {
        return providerCode;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public String getFailureReason() {
        return failureReason;
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
}
