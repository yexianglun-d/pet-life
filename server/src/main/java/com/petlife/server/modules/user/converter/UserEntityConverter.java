package com.petlife.server.modules.user.converter;

import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.dataobject.FamilySummaryDataObject;
import com.petlife.server.modules.user.persistence.dataobject.UserProfileDataObject;
import org.springframework.stereotype.Component;

/**
 * 用户域实体转换器。
 */
@Component
public class UserEntityConverter {

    public UserProfileEntity toEntity(UserProfileDataObject userProfileDataObject) {
        if (userProfileDataObject == null) {
            return null;
        }

        return new UserProfileEntity(
            userProfileDataObject.userId(),
            userProfileDataObject.mobile(),
            userProfileDataObject.nickname(),
            userProfileDataObject.avatarUrl(),
            userProfileDataObject.cityCode(),
            userProfileDataObject.cityName(),
            userProfileDataObject.currentPetId()
        );
    }

    public FamilySummaryEntity toEntity(FamilySummaryDataObject familySummaryDataObject) {
        if (familySummaryDataObject == null) {
            return null;
        }

        return new FamilySummaryEntity(
            familySummaryDataObject.familyId(),
            familySummaryDataObject.familyName(),
            familySummaryDataObject.memberCount(),
            familySummaryDataObject.role()
        );
    }
}
