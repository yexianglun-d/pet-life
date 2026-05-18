package com.petlife.server.modules.user.persistence.command;

/**
 * 用户通知设置更新命令。
 */
public class UpdateUserNotificationSettingsCommand {

    private Long userId;
    private Integer notificationSwitch;
    private String privacyLevel;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getNotificationSwitch() {
        return notificationSwitch;
    }

    public void setNotificationSwitch(Integer notificationSwitch) {
        this.notificationSwitch = notificationSwitch;
    }

    public String getPrivacyLevel() {
        return privacyLevel;
    }

    public void setPrivacyLevel(String privacyLevel) {
        this.privacyLevel = privacyLevel;
    }
}
