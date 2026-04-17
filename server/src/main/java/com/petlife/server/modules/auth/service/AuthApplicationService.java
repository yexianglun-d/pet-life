package com.petlife.server.modules.auth.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.dto.request.AuthSmsLoginRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.dto.response.AuthFamilySummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthLoginSmsResponse;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthSmsSendResponse;
import com.petlife.server.modules.auth.dto.response.AuthUserResponse;
import com.petlife.server.modules.auth.token.AccessTokenRepository;
import com.petlife.server.modules.auth.token.IssuedLoginTokens;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.persistence.record.PetProfilePersistenceRecord;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.user.persistence.command.CreateUserCommand;
import com.petlife.server.modules.user.persistence.record.FamilySummaryPersistenceRecord;
import com.petlife.server.modules.user.persistence.record.UserProfilePersistenceRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String DEVELOPMENT_MOBILE = "13800000000";

    private final AccessTokenRepository accessTokenRepository;
    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;

    public AuthApplicationService(
        AccessTokenRepository accessTokenRepository,
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper
    ) {
        this.accessTokenRepository = accessTokenRepository;
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
    }

    public AuthSmsSendResponse sendSmsCode(AuthSmsSendRequest request) {
        return new AuthSmsSendResponse(
            request.mobile(),
            request.scene(),
            DEVELOPMENT_SMS_CODE,
            300
        );
    }

    @Transactional
    public AuthLoginSmsResponse loginBySms(AuthSmsLoginRequest request) {
        if (!DEVELOPMENT_SMS_CODE.equals(request.code())) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_INVALID);
        }

        UserProfilePersistenceRecord userProfile = ensureUserProfile(request.mobile());
        FamilySummaryPersistenceRecord familySummary = ensurePrimaryFamily(userProfile);
        userPersistenceMapper.insertUserSettingsIfAbsent(userProfile.userId());
        ensureDefaultPetIfNecessary(userProfile, familySummary);
        userProfile = userPersistenceMapper.findUserProfileById(userProfile.userId());

        List<AuthPetSummaryResponse> pets = petPersistenceMapper.listPetsByUserId(userProfile.userId()).stream()
            .map(this::toPetSummary)
            .toList();
        IssuedLoginTokens issuedLoginTokens = accessTokenRepository.issueLoginTokens(userProfile.userId());

        return new AuthLoginSmsResponse(
            issuedLoginTokens.accessToken(),
            issuedLoginTokens.refreshToken(),
            toUserResponse(userProfile),
            toFamilySummaryResponse(familySummary),
            pets,
            userProfile.currentPetId() == null ? null : String.valueOf(userProfile.currentPetId())
        );
    }

    public AuthUserResponse toUserResponse(UserProfilePersistenceRecord userProfile) {
        return new AuthUserResponse(
            String.valueOf(userProfile.userId()),
            userProfile.mobile(),
            userProfile.nickname(),
            userProfile.cityCode(),
            userProfile.cityName()
        );
    }

    public AuthFamilySummaryResponse toFamilySummaryResponse(FamilySummaryPersistenceRecord familySummary) {
        return new AuthFamilySummaryResponse(
            String.valueOf(familySummary.familyId()),
            familySummary.familyName(),
            familySummary.memberCount(),
            familySummary.role()
        );
    }

    public AuthPetSummaryResponse toPetSummary(PetProfilePersistenceRecord petProfile) {
        return new AuthPetSummaryResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed()
        );
    }

    private UserProfilePersistenceRecord ensureUserProfile(String mobile) {
        UserProfilePersistenceRecord existingUser = userPersistenceMapper.findUserProfileByMobile(mobile);
        if (existingUser != null) {
            userPersistenceMapper.updateLastLoginAt(existingUser.userId());
            return existingUser;
        }

        CreateUserCommand command = new CreateUserCommand();
        command.setMobile(mobile);
        command.setNickname(DEVELOPMENT_MOBILE.equals(mobile) ? "Momo" : "宠物家长");
        command.setCityCode("310100");
        command.setCityName("上海");
        userPersistenceMapper.insertUser(command);
        return userPersistenceMapper.findUserProfileById(command.getId());
    }

    private FamilySummaryPersistenceRecord ensurePrimaryFamily(UserProfilePersistenceRecord userProfile) {
        FamilySummaryPersistenceRecord existingFamily =
            userPersistenceMapper.findPrimaryFamilySummaryByUserId(userProfile.userId());
        if (existingFamily != null) {
            return existingFamily;
        }

        CreateFamilyCommand command = new CreateFamilyCommand();
        command.setOwnerUserId(userProfile.userId());
        command.setFamilyName(userProfile.nickname() + "的家庭");
        userPersistenceMapper.insertFamily(command);
        userPersistenceMapper.insertFamilyMember(command.getId(), userProfile.userId(), "owner");
        return userPersistenceMapper.findPrimaryFamilySummaryByUserId(userProfile.userId());
    }

    private void ensureDefaultPetIfNecessary(
        UserProfilePersistenceRecord userProfile,
        FamilySummaryPersistenceRecord familySummary
    ) {
        List<PetProfilePersistenceRecord> pets = petPersistenceMapper.listPetsByUserId(userProfile.userId());
        if (pets.isEmpty()) {
            CreatePetCommand command = new CreatePetCommand();
            command.setFamilyId(familySummary.familyId());
            command.setOwnerUserId(userProfile.userId());
            command.setPetName(DEVELOPMENT_MOBILE.equals(userProfile.mobile()) ? "Momo" : "宠物宝宝");
            command.setPetType("cat");
            command.setBreed("British Shorthair");
            command.setGender("female");
            command.setBirthday(LocalDate.of(2023, 5, 20));
            command.setAdoptDate(LocalDate.of(2023, 8, 1));
            command.setNeuterStatus(1);
            petPersistenceMapper.insertPet(command);
            userPersistenceMapper.updateCurrentPet(userProfile.userId(), command.getId());
            return;
        }

        if (userProfile.currentPetId() == null) {
            userPersistenceMapper.updateCurrentPet(userProfile.userId(), pets.get(0).petId());
        }
    }
}
