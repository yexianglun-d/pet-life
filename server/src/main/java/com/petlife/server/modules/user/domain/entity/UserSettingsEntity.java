package com.petlife.server.modules.user.domain.entity;

/**
 * 用户设置实体。
 *
 * <p>该实体承接“我的 / 设置”页需要的稳定上下文，
 * 统一组合用户资料、城市偏好与通知偏好，避免页面侧再从多份响应中拼装设置状态。</p>
 */
public final class UserSettingsEntity {

    private final Long userId;
    private final String mobile;
    private final String nickname;
    private final String cityCode;
    private final String cityName;
    private final Long currentPetId;
    private final boolean notificationEnabled;
    private final String privacyLevel;

    public UserSettingsEntity(
        Long userId,
        String mobile,
        String nickname,
        String cityCode,
        String cityName,
        Long currentPetId,
        boolean notificationEnabled,
        String privacyLevel
    ) {
        this.userId = userId;
        this.mobile = mobile;
        this.nickname = nickname;
        this.cityCode = cityCode;
        this.cityName = cityName;
        this.currentPetId = currentPetId;
        this.notificationEnabled = notificationEnabled;
        this.privacyLevel = privacyLevel;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMobile() {
        return mobile;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public Long getCurrentPetId() {
        return currentPetId;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public String getPrivacyLevel() {
        return privacyLevel;
    }
}
