package com.petlife.server.modules.auth.converter;

import com.petlife.server.modules.auth.dto.response.AuthFamilySummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthUserResponse;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import org.springframework.stereotype.Component;

/**
 * 认证域视图转换器。
 *
 * <p>认证接口返回的用户、家庭和宠物摘要在多个服务间复用，
 * 统一在该转换器中完成响应组装，避免业务服务互相暴露内部转换细节。</p>
 */
@Component
public class AuthResponseConverter {

    public AuthUserResponse toUserResponse(UserProfileEntity userProfile) {
        return new AuthUserResponse(
            String.valueOf(userProfile.getUserId()),
            userProfile.getMobile(),
            userProfile.getNickname(),
            userProfile.getCityCode(),
            userProfile.getCityName()
        );
    }

    public AuthFamilySummaryResponse toFamilySummaryResponse(FamilySummaryEntity familySummary) {
        return new AuthFamilySummaryResponse(
            String.valueOf(familySummary.getFamilyId()),
            familySummary.getFamilyName(),
            familySummary.getMemberCount(),
            familySummary.getRole()
        );
    }

    public AuthPetSummaryResponse toPetSummary(PetProfileEntity petProfile) {
        return new AuthPetSummaryResponse(
            String.valueOf(petProfile.getPetId()),
            petProfile.getPetName(),
            petProfile.getPetType(),
            petProfile.getBreed()
        );
    }
}
