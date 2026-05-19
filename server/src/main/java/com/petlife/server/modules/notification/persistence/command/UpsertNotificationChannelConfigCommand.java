package com.petlife.server.modules.notification.persistence.command;

/**
 * 通知渠道配置创建或更新命令。
 */
public class UpsertNotificationChannelConfigCommand {

    private Long channelConfigId;
    private String channelType;
    private String providerCode;
    private String providerName;
    private boolean enabled;
    private String configStatus;
    private String remark;

    public Long getChannelConfigId() {
        return channelConfigId;
    }

    public void setChannelConfigId(Long channelConfigId) {
        this.channelConfigId = channelConfigId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
