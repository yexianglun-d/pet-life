package com.petlife.server.modules.admin.converter;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;
import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;
import org.springframework.stereotype.Component;

/**
 * 后台通用上下文转换器。
 */
@Component
public class AdminContextConverter {

    public AdminPetContextResponse toPetResponse(AdminPetContextEntity petContext) {
        if (petContext == null) {
            return null;
        }
        return new AdminPetContextResponse(
            String.valueOf(petContext.getPetId()),
            petContext.getPetName(),
            petContext.getPetType(),
            petContext.getFamilyId() == null ? null : String.valueOf(petContext.getFamilyId()),
            petContext.getFamilyName(),
            petContext.getOwnerUserId() == null ? null : String.valueOf(petContext.getOwnerUserId()),
            petContext.getOwnerNickname(),
            petContext.getOwnerMobile()
        );
    }

    public AdminUserContextResponse toUserResponse(AdminUserContextEntity userContext) {
        if (userContext == null || userContext.getUserId() == null) {
            return null;
        }
        return new AdminUserContextResponse(
            String.valueOf(userContext.getUserId()),
            userContext.getNickname(),
            userContext.getMobile()
        );
    }
}
