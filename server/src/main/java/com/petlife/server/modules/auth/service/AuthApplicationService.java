package com.petlife.server.modules.auth.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevFamilySummary;
import com.petlife.server.bootstrap.devsupport.model.DevPetProfile;
import com.petlife.server.bootstrap.devsupport.model.DevUserProfile;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.dto.request.AuthSmsLoginRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.dto.response.AuthFamilySummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthLoginSmsResponse;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthSmsSendResponse;
import com.petlife.server.modules.auth.dto.response.AuthUserResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务。
 *
 * <p>当前阶段使用固定验证码 `123456` 作为开发联调用验证码，
 * 目的是先把登录流程、前后端契约和页面跳转打通。正式短信接入后，
 * 该服务将替换为真实验证码校验与 token 签发逻辑。</p>
 */
@Service
public class AuthApplicationService {

    private static final String DEVELOPMENT_SMS_CODE = "123456";

    private final BootstrapMemoryStore bootstrapMemoryStore;

    public AuthApplicationService(BootstrapMemoryStore bootstrapMemoryStore) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
    }

    public AuthSmsSendResponse sendSmsCode(AuthSmsSendRequest request) {
        return new AuthSmsSendResponse(
            request.mobile(),
            request.scene(),
            DEVELOPMENT_SMS_CODE,
            300
        );
    }

    public AuthLoginSmsResponse loginBySms(AuthSmsLoginRequest request) {
        if (!DEVELOPMENT_SMS_CODE.equals(request.code())) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_INVALID);
        }

        DevUserProfile userProfile = bootstrapMemoryStore.getCurrentUser();
        DevFamilySummary familySummary = bootstrapMemoryStore.getFamilySummary();
        List<AuthPetSummaryResponse> pets = bootstrapMemoryStore.listPets().stream()
            .map(this::toPetSummary)
            .toList();

        return new AuthLoginSmsResponse(
            bootstrapMemoryStore.generateToken(),
            bootstrapMemoryStore.generateToken(),
            toUserResponse(userProfile),
            toFamilySummaryResponse(familySummary),
            pets,
            String.valueOf(userProfile.currentPetId())
        );
    }

    public AuthUserResponse toUserResponse(DevUserProfile userProfile) {
        return new AuthUserResponse(
            String.valueOf(userProfile.userId()),
            userProfile.mobile(),
            userProfile.nickname(),
            userProfile.cityCode(),
            userProfile.cityName()
        );
    }

    public AuthFamilySummaryResponse toFamilySummaryResponse(DevFamilySummary familySummary) {
        return new AuthFamilySummaryResponse(
            String.valueOf(familySummary.familyId()),
            familySummary.familyName(),
            familySummary.memberCount(),
            familySummary.role()
        );
    }

    public AuthPetSummaryResponse toPetSummary(DevPetProfile petProfile) {
        return new AuthPetSummaryResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed()
        );
    }
}
