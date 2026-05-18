package com.petlife.server.modules.user.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台用户查询数据对象。
 */
public record AdminUserDataObject(
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
    Integer notificationSwitch,
    String privacyLevel,
    Long familyId,
    String familyName,
    String familyRole,
    Integer familyMemberCount,
    Long currentPetFamilyId,
    String currentPetFamilyName,
    String currentPetName,
    String currentPetType,
    Long currentPetOwnerUserId,
    String currentPetOwnerNickname,
    String currentPetOwnerMobile,
    Integer petCount
) {
}
