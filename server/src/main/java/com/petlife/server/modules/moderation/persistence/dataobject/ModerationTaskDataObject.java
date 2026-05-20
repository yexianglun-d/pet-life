package com.petlife.server.modules.moderation.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 内容审核任务数据对象。
 */
public record ModerationTaskDataObject(
    Long taskId,
    String targetType,
    Long targetId,
    String contentType,
    String contentSnapshot,
    String providerCode,
    String reviewStatus,
    String reviewResult,
    String riskLabels,
    String failureReason,
    String callbackPayload,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
