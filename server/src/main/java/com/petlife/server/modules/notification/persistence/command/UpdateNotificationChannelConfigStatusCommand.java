package com.petlife.server.modules.notification.persistence.command;

/**
 * 更新通知渠道启停状态命令。
 */
public class UpdateNotificationChannelConfigStatusCommand {

    private Long channelConfigId;
    private boolean enabled;
    private String configStatus;

    public Long getChannelConfigId() {
        return channelConfigId;
    }

    public void setChannelConfigId(Long channelConfigId) {
        this.channelConfigId = channelConfigId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigStatus() {
        return configStatus;
    }

    public void setConfigStatus(String configStatus) {
        this.configStatus = configStatus;
    }
}
