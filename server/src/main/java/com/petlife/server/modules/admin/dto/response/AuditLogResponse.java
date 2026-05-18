package com.petlife.server.modules.admin.dto.response;

import java.time.OffsetDateTime;

/**
 * 后台审计日志响应。
 *
 * @param auditLogId 审计日志 ID
 * @param operatorType 操作者类型
 * @param operatorId 操作者标识
 * @param targetType 目标类型
 * @param targetId 目标 ID
 * @param action 操作动作
 * @param detailJson 操作详情 JSON
 * @param ipAddress 操作 IP
 * @param userAgent 客户端标识
 * @param createdAt 创建时间
 */
public record AuditLogResponse(
    String auditLogId,
    String operatorType,
    String operatorId,
    String targetType,
    String targetId,
    String action,
    String detailJson,
    String ipAddress,
    String userAgent,
    OffsetDateTime createdAt
) {
}
