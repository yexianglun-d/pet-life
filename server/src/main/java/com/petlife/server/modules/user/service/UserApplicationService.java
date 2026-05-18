package com.petlife.server.modules.user.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.auth.persistence.AuthTokenPersistenceMapper;
import com.petlife.server.modules.auth.converter.AuthResponseConverter;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.AdminUserConverter;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.converter.UserSettingsConverter;
import com.petlife.server.modules.user.domain.entity.AdminUserEntity;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.dto.request.AdminUpdateUserStatusRequest;
import com.petlife.server.modules.user.dto.response.AdminUserResponse;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import com.petlife.server.modules.user.dto.response.UserSettingsResponse;
import com.petlife.server.modules.user.persistence.command.UpdateUserCityCommand;
import com.petlife.server.modules.user.persistence.command.UpdateUserNotificationSettingsCommand;
import com.petlife.server.modules.user.persistence.command.UpdateUserProfileCommand;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前用户应用服务。
 *
 * <p>该服务负责输出当前登录用户的稳定上下文，包括用户本人、当前宠物与所属家庭，
 * 并统一处理当前宠物切换动作。</p>
 */
@Service
public class UserApplicationService {

    private final AuthResponseConverter authResponseConverter;
    private final AdminUserConverter adminUserConverter;
    private final UserEntityConverter userEntityConverter;
    private final UserSettingsConverter userSettingsConverter;
    private final PetEntityConverter petEntityConverter;
    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final UserBootstrapApplicationService userBootstrapApplicationService;
    private final AuthTokenPersistenceMapper authTokenPersistenceMapper;
    private final AuditLogApplicationService auditLogApplicationService;

    public UserApplicationService(
        AuthResponseConverter authResponseConverter,
        AdminUserConverter adminUserConverter,
        UserEntityConverter userEntityConverter,
        UserSettingsConverter userSettingsConverter,
        PetEntityConverter petEntityConverter,
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        UserBootstrapApplicationService userBootstrapApplicationService,
        AuthTokenPersistenceMapper authTokenPersistenceMapper,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.authResponseConverter = authResponseConverter;
        this.adminUserConverter = adminUserConverter;
        this.userEntityConverter = userEntityConverter;
        this.userSettingsConverter = userSettingsConverter;
        this.petEntityConverter = petEntityConverter;
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
        this.authTokenPersistenceMapper = authTokenPersistenceMapper;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public CurrentUserResponse getCurrentUser() {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilySummaryEntity familySummary = userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(currentUserId);
        UserProfileEntity currentUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(currentUserId));
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "当前用户不存在");
        }

        PetProfileEntity currentPet = null;
        if (currentUser.getCurrentPetId() != null) {
            currentPet = petEntityConverter.toEntity(
                petPersistenceMapper.findAccessiblePetById(currentUserId, currentUser.getCurrentPetId())
            );
        }

        return new CurrentUserResponse(
            authResponseConverter.toUserResponse(currentUser),
            currentUser.getCurrentPetId() == null ? null : String.valueOf(currentUser.getCurrentPetId()),
            currentPet == null ? null : authResponseConverter.toPetSummary(currentPet),
            authResponseConverter.toFamilySummaryResponse(familySummary)
        );
    }

    @Transactional
    public CurrentUserResponse updateCurrentPet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }

        userPersistenceMapper.updateCurrentPet(currentUserId, petId);
        return getCurrentUser();
    }

    public UserSettingsResponse getUserSettings() {
        Long currentUserId = CurrentUserContext.requireUserId();
        userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(currentUserId);
        return loadUserSettings(currentUserId);
    }

    public List<AdminUserResponse> listAdminUsers(
        String keyword,
        String mobile,
        String nickname,
        String cityCode,
        Boolean notificationEnabled,
        String privacyLevel
    ) {
        String normalizedKeyword = normalizeOptionalText(keyword, 100, "搜索关键词长度不能超过 100 个字符");
        String normalizedMobile = normalizeOptionalText(mobile, 20, "手机号长度不能超过 20 个字符");
        String normalizedNickname = normalizeOptionalText(nickname, 50, "昵称长度不能超过 50 个字符");
        String normalizedCityCode = normalizeOptionalText(cityCode, 32, "城市编码长度不能超过 32 个字符");
        String normalizedPrivacyLevel = normalizeOptionalPrivacyLevel(privacyLevel);
        Integer notificationSwitch = notificationEnabled == null ? null : (notificationEnabled ? 1 : 0);

        // 后台查询只读取真实用户数据，不触发用户初始化，避免管理端查询改变业务事实。
        return userPersistenceMapper
            .listAdminUsers(
                normalizedKeyword,
                normalizedMobile,
                normalizedNickname,
                normalizedCityCode,
                notificationSwitch,
                normalizedPrivacyLevel
            )
            .stream()
            .map(adminUserConverter::toEntity)
            .map(adminUserConverter::toResponse)
            .toList();
    }

    public AdminUserResponse getAdminUser(Long userId) {
        AdminUserEntity adminUser = adminUserConverter.toEntity(userPersistenceMapper.findAdminUserById(userId));
        if (adminUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return adminUserConverter.toResponse(adminUser);
    }

    /**
     * 后台用户状态治理只允许在“正常”和“禁用”之间切换。
     *
     * <p>禁用用户后必须同步吊销其用户端会话，否则用户在 token 未过期前仍可继续访问受保护接口。</p>
     */
    @Transactional
    public AdminUserResponse updateAdminUserStatus(
        Long userId,
        AdminOperationContext operationContext,
        AdminUpdateUserStatusRequest request
    ) {
        AdminUserEntity adminUser = adminUserConverter.toEntity(userPersistenceMapper.findAdminUserById(userId));
        if (adminUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        Integer targetStatus = normalizeAdminUserStatus(request.status());
        if (adminUser.getStatus().equals(targetStatus)) {
            return adminUserConverter.toResponse(adminUser);
        }

        int updatedRows = userPersistenceMapper.updateUserStatus(userId, targetStatus);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        if (Integer.valueOf(2).equals(targetStatus)) {
            authTokenPersistenceMapper.revokeSessionsByUserId(userId);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from_status", adminUser.getStatus());
        detail.put("to_status", targetStatus);
        detail.put("reason", normalizeNullableText(request.reason()));
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            "user",
            String.valueOf(userId),
            Integer.valueOf(2).equals(targetStatus) ? "user_disable" : "user_restore",
            detail
        );
        return getAdminUser(userId);
    }

    /**
     * 用户资料更新只允许变更当前阶段真正有落地能力的字段。
     *
     * <p>当前先开放昵称编辑，不把头像等尚未接入媒体上传的能力提前暴露成半成品接口。</p>
     */
    @Transactional
    public UserSettingsResponse updateUserProfile(String nickname) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UpdateUserProfileCommand command = new UpdateUserProfileCommand();
        command.setUserId(currentUserId);
        command.setNickname(normalizeNickname(nickname));
        userPersistenceMapper.updateUserProfile(command);
        return loadUserSettings(currentUserId);
    }

    @Transactional
    public UserSettingsResponse updateUserCity(String cityCode, String cityName) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UpdateUserCityCommand command = new UpdateUserCityCommand();
        command.setUserId(currentUserId);
        command.setCityCode(normalizeRequiredText(cityCode, "城市编码不能为空"));
        command.setCityName(normalizeRequiredText(cityName, "城市名称不能为空"));
        userPersistenceMapper.updateUserCity(command);
        return loadUserSettings(currentUserId);
    }

    /**
     * 通知设置在真正接入推送链路前，先作为用户级偏好统一落库。
     *
     * <p>这样后续通知投递、消息中心未读态和服务预约开通通知都能共用同一份用户偏好，不需要再做迁移补洞。</p>
     */
    @Transactional
    public UserSettingsResponse updateUserNotificationSettings(boolean notificationEnabled, String privacyLevel) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UpdateUserNotificationSettingsCommand command = new UpdateUserNotificationSettingsCommand();
        command.setUserId(currentUserId);
        command.setNotificationSwitch(notificationEnabled ? 1 : 0);
        command.setPrivacyLevel(normalizePrivacyLevel(privacyLevel));
        userPersistenceMapper.updateUserNotificationSettings(command);
        return loadUserSettings(currentUserId);
    }

    private UserSettingsResponse loadUserSettings(Long currentUserId) {
        var userSettingsEntity = userSettingsConverter.toEntity(userPersistenceMapper.findUserSettingsByUserId(currentUserId));
        if (userSettingsEntity == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "当前用户设置不存在");
        }
        return userSettingsConverter.toResponse(userSettingsEntity);
    }

    private String normalizeNickname(String nickname) {
        String normalizedNickname = normalizeRequiredText(nickname, "昵称不能为空");
        if (normalizedNickname.length() > 50) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "昵称长度不能超过 50 个字符");
        }
        return normalizedNickname;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value, int maxLength, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, errorMessage);
        }
        return normalizedValue;
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePrivacyLevel(String privacyLevel) {
        String normalizedPrivacyLevel = normalizeRequiredText(privacyLevel, "隐私级别不能为空").toLowerCase();
        if (!"normal".equals(normalizedPrivacyLevel) && !"private".equals(normalizedPrivacyLevel)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "隐私级别仅支持 normal 或 private");
        }
        return normalizedPrivacyLevel;
    }

    private String normalizeOptionalPrivacyLevel(String privacyLevel) {
        String normalizedPrivacyLevel = normalizeOptionalText(privacyLevel, 20, "隐私级别长度不能超过 20 个字符");
        if (normalizedPrivacyLevel == null) {
            return null;
        }
        normalizedPrivacyLevel = normalizedPrivacyLevel.toLowerCase();
        if (!"normal".equals(normalizedPrivacyLevel) && !"private".equals(normalizedPrivacyLevel)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "隐私级别仅支持 normal 或 private");
        }
        return normalizedPrivacyLevel;
    }

    private Integer normalizeAdminUserStatus(Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户状态仅支持 1 或 2");
        }
        return status;
    }
}
