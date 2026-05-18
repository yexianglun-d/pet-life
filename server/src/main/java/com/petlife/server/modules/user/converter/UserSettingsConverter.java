package com.petlife.server.modules.user.converter;

import com.petlife.server.modules.user.domain.entity.UserSettingsEntity;
import com.petlife.server.modules.user.dto.response.UserSettingsResponse;
import com.petlife.server.modules.user.persistence.dataobject.UserSettingsDataObject;
import org.springframework.stereotype.Component;

/**
 * 用户设置转换器。
 */
@Component
public class UserSettingsConverter {

    public UserSettingsEntity toEntity(UserSettingsDataObject userSettingsDataObject) {
        if (userSettingsDataObject == null) {
            return null;
        }

        return new UserSettingsEntity(
            userSettingsDataObject.userId(),
            userSettingsDataObject.mobile(),
            userSettingsDataObject.nickname(),
            userSettingsDataObject.cityCode(),
            userSettingsDataObject.cityName(),
            userSettingsDataObject.currentPetId(),
            userSettingsDataObject.notificationSwitch() != null && userSettingsDataObject.notificationSwitch() == 1,
            userSettingsDataObject.privacyLevel()
        );
    }

    public UserSettingsResponse toResponse(UserSettingsEntity userSettingsEntity) {
        if (userSettingsEntity == null) {
            return null;
        }

        return new UserSettingsResponse(
            String.valueOf(userSettingsEntity.getUserId()),
            userSettingsEntity.getMobile(),
            userSettingsEntity.getNickname(),
            userSettingsEntity.getCityCode(),
            userSettingsEntity.getCityName(),
            userSettingsEntity.getCurrentPetId() == null ? null : String.valueOf(userSettingsEntity.getCurrentPetId()),
            userSettingsEntity.isNotificationEnabled(),
            userSettingsEntity.getPrivacyLevel()
        );
    }
}
