package com.petlife.server.modules.admin.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台审计日志持久化读模型。
 */
public record AuditLogDataObject(
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
}
