package com.petlife.server.modules.notification.persistence.command;

/**
 * 注册或更新 Push 设备 Token 命令。
 */
public class UpsertPushDeviceTokenCommand {

    private Long deviceTokenId;
    private Long userId;
    private String platform;
    private String providerCode;
    private String deviceToken;
    private String deviceId;
    private String appVersion;

    public Long getDeviceTokenId() {
        return deviceTokenId;
    }

    public void setDeviceTokenId(Long deviceTokenId) {
        this.deviceTokenId = deviceTokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
}
