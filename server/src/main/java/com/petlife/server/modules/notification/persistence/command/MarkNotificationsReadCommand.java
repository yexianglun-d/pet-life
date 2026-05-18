package com.petlife.server.modules.notification.persistence.command;

/**
 * 批量通知已读命令。
 */
public class MarkNotificationsReadCommand {

    private Long userId;
    private String notifyType;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
    }
}
