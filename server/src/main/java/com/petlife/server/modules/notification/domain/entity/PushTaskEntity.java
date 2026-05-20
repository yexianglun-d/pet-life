package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * Push 任务实体。
 */
public final class PushTaskEntity {

    private final Long pushTaskId;
    private final Long userId;
    private final Long notificationId;
    private final String notifyType;
    private final String bizType;
    private final Long bizId;
    private final String title;
    private final String content;
    private final String providerCode;
    private final String taskStatus;
    private final String failureReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PushTaskEntity(
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
        this.pushTaskId = pushTaskId;
        this.userId = userId;
        this.notificationId = notificationId;
        this.notifyType = notifyType;
        this.bizType = bizType;
        this.bizId = bizId;
        this.title = title;
        this.content = content;
        this.providerCode = providerCode;
        this.taskStatus = taskStatus;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getPushTaskId() {
        return pushTaskId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public String getBizType() {
        return bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
