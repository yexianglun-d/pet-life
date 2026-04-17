package com.petlife.server.modules.user.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.auth.service.AuthApplicationService;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.record.PetProfilePersistenceRecord;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.persistence.record.FamilySummaryPersistenceRecord;
import com.petlife.server.modules.user.persistence.record.UserProfilePersistenceRecord;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务。
 */
@Service
public class UserApplicationService {

    private final AuthApplicationService authApplicationService;
    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;

    public UserApplicationService(
        AuthApplicationService authApplicationService,
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper
    ) {
        this.authApplicationService = authApplicationService;
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
    }

    public CurrentUserResponse getCurrentUser() {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfilePersistenceRecord currentUser = userPersistenceMapper.findUserProfileById(currentUserId);
        if (currentUser == null || currentUser.currentPetId() == null) {
            throw new BusinessException(ResponseCode.USER_CURRENT_PET_NOT_FOUND);
        }

        PetProfilePersistenceRecord currentPet =
            petPersistenceMapper.findAccessiblePetById(currentUserId, currentUser.currentPetId());
        if (currentPet == null) {
            throw new BusinessException(ResponseCode.USER_CURRENT_PET_NOT_FOUND);
        }

        FamilySummaryPersistenceRecord familySummary =
            userPersistenceMapper.findPrimaryFamilySummaryByUserId(currentUserId);

        return new CurrentUserResponse(
            authApplicationService.toUserResponse(currentUser),
            String.valueOf(currentUser.currentPetId()),
            toCurrentPetSummary(currentPet),
            authApplicationService.toFamilySummaryResponse(familySummary)
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

    private AuthPetSummaryResponse toCurrentPetSummary(PetProfilePersistenceRecord petProfile) {
        return new AuthPetSummaryResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed()
        );
    }
}
