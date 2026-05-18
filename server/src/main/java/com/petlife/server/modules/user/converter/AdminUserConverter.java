package com.petlife.server.modules.user.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.user.domain.entity.AdminUserEntity;
import com.petlife.server.modules.user.dto.response.AdminUserFamilyResponse;
import com.petlife.server.modules.user.dto.response.AdminUserResponse;
import com.petlife.server.modules.user.dto.response.AdminUserSettingsResponse;
import com.petlife.server.modules.user.persistence.dataobject.AdminUserDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台用户转换器。
 */
@Component
public class AdminUserConverter {

    private final AdminContextConverter adminContextConverter;

    public AdminUserConverter(AdminContextConverter adminContextConverter) {
        this.adminContextConverter = adminContextConverter;
    }

    public AdminUserEntity toEntity(AdminUserDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        AdminPetContextEntity currentPetContext = null;
        if (dataObject.currentPetId() != null) {
            currentPetContext = new AdminPetContextEntity(
                dataObject.currentPetId(),
                dataObject.currentPetName(),
                dataObject.currentPetType(),
                dataObject.currentPetFamilyId(),
                dataObject.currentPetFamilyName(),
                dataObject.currentPetOwnerUserId(),
                dataObject.currentPetOwnerNickname(),
                dataObject.currentPetOwnerMobile()
            );
        }
        return new AdminUserEntity(
            dataObject.userId(),
            dataObject.mobile(),
            dataObject.nickname(),
            dataObject.avatarUrl(),
            dataObject.cityCode(),
            dataObject.cityName(),
            dataObject.status(),
            dataObject.lastLoginAt(),
            dataObject.createdAt(),
            dataObject.currentPetId(),
            dataObject.notificationSwitch() == null || dataObject.notificationSwitch() == 1,
            dataObject.privacyLevel() == null ? "normal" : dataObject.privacyLevel(),
            dataObject.familyId(),
            dataObject.familyName(),
            dataObject.familyRole(),
            dataObject.familyMemberCount(),
            currentPetContext,
            dataObject.petCount() == null ? 0 : dataObject.petCount()
        );
    }

    public AdminUserResponse toResponse(AdminUserEntity entity) {
        return new AdminUserResponse(
            String.valueOf(entity.getUserId()),
            entity.getMobile(),
            entity.getNickname(),
            entity.getAvatarUrl(),
            entity.getCityCode(),
            entity.getCityName(),
            entity.getStatus(),
            DateTimeConverters.toOffsetDateTime(entity.getLastLoginAt()),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            new AdminUserSettingsResponse(
                entity.getCurrentPetId() == null ? null : String.valueOf(entity.getCurrentPetId()),
                entity.isNotificationEnabled(),
                entity.getPrivacyLevel()
            ),
            entity.getFamilyId() == null ? null : new AdminUserFamilyResponse(
                String.valueOf(entity.getFamilyId()),
                entity.getFamilyName(),
                entity.getFamilyRole(),
                entity.getFamilyMemberCount()
            ),
            adminContextConverter.toPetResponse(entity.getCurrentPetContext()),
            entity.getPetCount()
        );
    }
}
