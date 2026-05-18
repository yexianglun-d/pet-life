package com.petlife.server.modules.admin.domain.entity;

import java.time.LocalDateTime;

/**
 * 后台审计日志实体。
 */
public class AuditLogEntity {

    private final Long auditLogId;
    private final String operatorType;
    private final String operatorId;
    private final String targetType;
    private final String targetId;
    private final String action;
    private final String detailJson;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime createdAt;

    public AuditLogEntity(
        Long auditLogId,
        String operatorType,
        String operatorId,
        String targetType,
        String targetId,
        String action,
        String detailJson,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
    ) {
        this.auditLogId = auditLogId;
        this.operatorType = operatorType;
        this.operatorId = operatorId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.detailJson = detailJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public Long getAuditLogId() {
        return auditLogId;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getAction() {
        return action;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
