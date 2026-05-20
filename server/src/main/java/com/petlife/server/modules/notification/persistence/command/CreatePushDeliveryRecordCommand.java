package com.petlife.server.modules.notification.persistence.command;

/**
 * 创建 Push 投递记录命令。
 */
public class CreatePushDeliveryRecordCommand {

    private Long pushTaskId;
    private Long deviceTokenId;
    private Long userId;
    private String providerCode;
    private String deliveryStatus;
    private String failureReason;

    public Long getPushTaskId() {
        return pushTaskId;
    }

    public void setPushTaskId(Long pushTaskId) {
        this.pushTaskId = pushTaskId;
    }

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

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
