package com.petlife.server.modules.user.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevPetProfile;
import com.petlife.server.bootstrap.devsupport.model.DevUserProfile;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.service.AuthApplicationService;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import org.springframework.stereotype.Service;

/**
 * 用户应用服务。
 */
@Service
public class UserApplicationService {

    private final BootstrapMemoryStore bootstrapMemoryStore;
    private final AuthApplicationService authApplicationService;

    public UserApplicationService(
        BootstrapMemoryStore bootstrapMemoryStore,
        AuthApplicationService authApplicationService
    ) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
        this.authApplicationService = authApplicationService;
    }

    public CurrentUserResponse getCurrentUser() {
        DevUserProfile currentUser = bootstrapMemoryStore.getCurrentUser();
        DevPetProfile currentPet = bootstrapMemoryStore.getPet(currentUser.currentPetId());

        return new CurrentUserResponse(
            authApplicationService.toUserResponse(currentUser),
            String.valueOf(currentUser.currentPetId()),
            toCurrentPetSummary(currentPet),
            authApplicationService.toFamilySummaryResponse(bootstrapMemoryStore.getFamilySummary())
        );
    }

    public CurrentUserResponse updateCurrentPet(Long petId) {
        bootstrapMemoryStore.updateCurrentPet(petId);
        return getCurrentUser();
    }

    private AuthPetSummaryResponse toCurrentPetSummary(DevPetProfile petProfile) {
        return new AuthPetSummaryResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed()
        );
    }
}
