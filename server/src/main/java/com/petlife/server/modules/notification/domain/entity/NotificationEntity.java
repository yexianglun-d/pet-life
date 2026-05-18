package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 *
 * <p>通知中心只消费这一个稳定实体，避免页面列表、已读动作和业务派发各自拼装数据库字段。</p>
 */
public final class NotificationEntity {

    private final Long notificationId;
    private final Long userId;
    private final String notifyType;
    private final String bizType;
    private final Long bizId;
    private final String title;
    private final String content;
    private final Integer readStatus;
    private final LocalDateTime sentAt;
    private final LocalDateTime readAt;

    public NotificationEntity(
        Long notificationId,
        Long userId,
        String notifyType,
        String bizType,
        Long bizId,
        String title,
        String content,
        Integer readStatus,
        LocalDateTime sentAt,
        LocalDateTime readAt
    ) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.notifyType = notifyType;
        this.bizType = bizType;
        this.bizId = bizId;
        this.title = title;
        this.content = content;
        this.readStatus = readStatus;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public Long getUserId() {
        return userId;
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

    public Integer getReadStatus() {
        return readStatus;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readStatus != null && readStatus == 1;
    }
}
