package com.petlife.server.modules.notification.dto.response;

import java.time.OffsetDateTime;

/**
 * Push 任务响应。
 */
public record PushTaskResponse(
    String pushTaskId,
    String userId,
    String notificationId,
    String notifyType,
    String bizType,
    String bizId,
    String title,
    String content,
    String providerCode,
    String taskStatus,
    String failureReason,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
