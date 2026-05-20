package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * Push 任务数据对象。
 */
public record PushTaskDataObject(
    Long pushTaskId,
    Long userId,
    Long notificationId,
    String notifyType,
    String bizType,
    Long bizId,
    String title,
    String content,
    String providerCode,
    String taskStatus,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
