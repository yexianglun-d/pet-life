package com.petlife.server.modules.auth.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.converter.AuthResponseConverter;
import com.petlife.server.modules.auth.dto.request.AuthLogoutRequest;
import com.petlife.server.modules.auth.dto.request.AuthRefreshTokenRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsLoginRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.dto.response.AuthRefreshTokenResponse;
import com.petlife.server.modules.auth.dto.response.AuthLoginSmsResponse;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthSmsSendResponse;
import com.petlife.server.modules.auth.token.AccessTokenRepository;
import com.petlife.server.modules.auth.token.IssuedLoginTokens;
import com.petlife.server.modules.notification.service.NotificationApplicationService;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.service.UserBootstrapApplicationService;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.persistence.command.CreateUserCommand;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务。
 *
 * <p>短信验证码由 SmsVerificationApplicationService 生成、摘要落库和状态校验。
 * AuthApplicationService 只负责认证编排、用户初始化和 token 签发。</p>
 */
@Service
public class AuthApplicationService {

    private static final String DEVELOPMENT_MOBILE = "13800000000";

    private final AccessTokenRepository accessTokenRepository;
    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final PetEntityConverter petEntityConverter;
    private final AuthResponseConverter authResponseConverter;
    private final UserBootstrapApplicationService userBootstrapApplicationService;
    private final NotificationApplicationService notificationApplicationService;
    private final SmsVerificationApplicationService smsVerificationApplicationService;

    public AuthApplicationService(
        AccessTokenRepository accessTokenRepository,
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        UserEntityConverter userEntityConverter,
        PetEntityConverter petEntityConverter,
        AuthResponseConverter authResponseConverter,
        UserBootstrapApplicationService userBootstrapApplicationService,
        NotificationApplicationService notificationApplicationService,
        SmsVerificationApplicationService smsVerificationApplicationService
    ) {
        this.accessTokenRepository = accessTokenRepository;
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.petEntityConverter = petEntityConverter;
        this.authResponseConverter = authResponseConverter;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
        this.notificationApplicationService = notificationApplicationService;
        this.smsVerificationApplicationService = smsVerificationApplicationService;
    }

    public AuthSmsSendResponse sendSmsCode(AuthSmsSendRequest request, HttpServletRequest httpServletRequest) {
        return smsVerificationApplicationService.sendSmsCode(request, httpServletRequest);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthLoginSmsResponse loginBySms(AuthSmsLoginRequest request) {
        smsVerificationApplicationService.verifyLoginCode(request.mobile(), request.code());
        UserProfileEntity userProfile = ensureUserProfile(request.mobile());
        // 登录成功后必须保证用户已经拥有可访问家庭和可用当前宠物，避免后续 `/me` 读取出现脏引用。
        FamilySummaryEntity familySummary =
            userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(userProfile.getUserId());
        notificationApplicationService.createWelcomeNotificationIfAbsent(userProfile.getUserId());
        userProfile = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(userProfile.getUserId()));

        List<AuthPetSummaryResponse> pets = petPersistenceMapper.listPetsByUserId(userProfile.getUserId()).stream()
            .map(petEntityConverter::toEntity)
            .map(authResponseConverter::toPetSummary)
            .toList();
        IssuedLoginTokens issuedLoginTokens = accessTokenRepository.issueLoginTokens(userProfile.getUserId());

        return new AuthLoginSmsResponse(
            issuedLoginTokens.accessToken(),
            issuedLoginTokens.refreshToken(),
            authResponseConverter.toUserResponse(userProfile),
            authResponseConverter.toFamilySummaryResponse(familySummary),
            pets,
            userProfile.getCurrentPetId() == null ? null : String.valueOf(userProfile.getCurrentPetId())
        );
    }

    @Transactional
    public AuthRefreshTokenResponse refreshToken(AuthRefreshTokenRequest request) {
        IssuedLoginTokens issuedLoginTokens = accessTokenRepository.refreshLoginTokens(request.refreshToken())
            .orElseThrow(() -> new BusinessException(
                ResponseCode.AUTH_REFRESH_TOKEN_INVALID,
                "登录状态已失效，请重新登录"
            ));
        return new AuthRefreshTokenResponse(
            issuedLoginTokens.accessToken(),
            issuedLoginTokens.refreshToken()
        );
    }

    @Transactional
    public void logout(AuthLogoutRequest request) {
        accessTokenRepository.revokeRefreshToken(request.refreshToken());
    }

    private UserProfileEntity ensureUserProfile(String mobile) {
        UserProfileEntity existingUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileByMobile(mobile));
        if (existingUser != null) {
            userPersistenceMapper.updateLastLoginAt(existingUser.getUserId());
            return existingUser;
        }

        CreateUserCommand command = new CreateUserCommand();
        command.setMobile(mobile);
        command.setNickname(DEVELOPMENT_MOBILE.equals(mobile) ? "Momo" : "宠物家长");
        command.setCityCode("310100");
        command.setCityName("上海");
        userPersistenceMapper.insertUser(command);
        return userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(command.getId()));
    }
}
