package com.petlife.server.modules.user.domain.entity;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import java.time.LocalDateTime;

/**
 * 后台用户聚合实体。
 */
public final class AdminUserEntity {

    private final Long userId;
    private final String mobile;
    private final String nickname;
    private final String avatarUrl;
    private final String cityCode;
    private final String cityName;
    private final Integer status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final Long currentPetId;
    private final boolean notificationEnabled;
    private final String privacyLevel;
    private final Long familyId;
    private final String familyName;
    private final String familyRole;
    private final Integer familyMemberCount;
    private final AdminPetContextEntity currentPetContext;
    private final Integer petCount;

    public AdminUserEntity(
        Long userId,
        String mobile,
        String nickname,
        String avatarUrl,
        String cityCode,
        String cityName,
        Integer status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        Long currentPetId,
        boolean notificationEnabled,
        String privacyLevel,
        Long familyId,
        String familyName,
        String familyRole,
        Integer familyMemberCount,
        AdminPetContextEntity currentPetContext,
        Integer petCount
    ) {
        this.userId = userId;
        this.mobile = mobile;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.cityCode = cityCode;
        this.cityName = cityName;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.currentPetId = currentPetId;
        this.notificationEnabled = notificationEnabled;
        this.privacyLevel = privacyLevel;
        this.familyId = familyId;
        this.familyName = familyName;
        this.familyRole = familyRole;
        this.familyMemberCount = familyMemberCount;
        this.currentPetContext = currentPetContext;
        this.petCount = petCount;
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

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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

    public Long getFamilyId() {
        return familyId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getFamilyRole() {
        return familyRole;
    }

    public Integer getFamilyMemberCount() {
        return familyMemberCount;
    }

    public AdminPetContextEntity getCurrentPetContext() {
        return currentPetContext;
    }

    public Integer getPetCount() {
        return petCount;
    }
}
