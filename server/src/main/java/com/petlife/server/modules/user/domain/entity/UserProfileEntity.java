package com.petlife.server.modules.user.domain.entity;

/**
 * 用户资料实体。
 *
 * <p>该实体承接当前登录用户在业务层需要的稳定信息，
 * 避免应用服务直接依赖底层持久化读取模型。</p>
 */
public final class UserProfileEntity {

    private final Long userId;
    private final String mobile;
    private final String nickname;
    private final String avatarUrl;
    private final String cityCode;
    private final String cityName;
    private final Long currentPetId;

    public UserProfileEntity(
        Long userId,
        String mobile,
        String nickname,
        String avatarUrl,
        String cityCode,
        String cityName,
        Long currentPetId
    ) {
        this.userId = userId;
        this.mobile = mobile;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.cityCode = cityCode;
        this.cityName = cityName;
        this.currentPetId = currentPetId;
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

    public String getAvatarUrl() {
        return avatarUrl;
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
}
