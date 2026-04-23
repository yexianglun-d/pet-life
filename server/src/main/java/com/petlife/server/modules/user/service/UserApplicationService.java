package com.petlife.server.modules.user.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.converter.AuthResponseConverter;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
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
    private final UserEntityConverter userEntityConverter;
    private final PetEntityConverter petEntityConverter;
    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final UserBootstrapApplicationService userBootstrapApplicationService;

    public UserApplicationService(
        AuthResponseConverter authResponseConverter,
        UserEntityConverter userEntityConverter,
        PetEntityConverter petEntityConverter,
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        UserBootstrapApplicationService userBootstrapApplicationService
    ) {
        this.authResponseConverter = authResponseConverter;
        this.userEntityConverter = userEntityConverter;
        this.petEntityConverter = petEntityConverter;
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
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
}
