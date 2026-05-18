package com.petlife.server.modules.notification.persistence.command;

/**
 * 单条通知已读命令。
 */
public class MarkNotificationReadCommand {

    private Long userId;
    private Long notificationId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
