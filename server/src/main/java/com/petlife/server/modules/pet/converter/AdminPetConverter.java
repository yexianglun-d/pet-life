package com.petlife.server.modules.pet.converter;

import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.pet.domain.entity.AdminPetEntity;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.dto.response.AdminPetFamilyResponse;
import com.petlife.server.modules.pet.dto.response.AdminPetResponse;
import com.petlife.server.modules.pet.persistence.dataobject.AdminPetDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台宠物转换器。
 */
@Component
public class AdminPetConverter {

    private final PetEntityConverter petEntityConverter;
    private final AdminContextConverter adminContextConverter;

    public AdminPetConverter(
        PetEntityConverter petEntityConverter,
        AdminContextConverter adminContextConverter
    ) {
        this.petEntityConverter = petEntityConverter;
        this.adminContextConverter = adminContextConverter;
    }

    public AdminPetEntity toEntity(AdminPetDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        PetProfileEntity petProfile = new PetProfileEntity(
            dataObject.petId(),
            dataObject.familyId(),
            dataObject.ownerUserId(),
            dataObject.petName(),
            dataObject.petType(),
            dataObject.breed(),
            dataObject.gender(),
            dataObject.birthday(),
            dataObject.adoptDate(),
            dataObject.neuterStatus(),
            dataObject.avatarUrl(),
            dataObject.weightKg(),
            dataObject.allergyNotes(),
            dataObject.medicalHistory(),
            dataObject.status(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
        return new AdminPetEntity(
            petProfile,
            dataObject.familyName(),
            dataObject.familyStatus(),
            dataObject.familyMemberCount() == null ? 0 : dataObject.familyMemberCount(),
            dataObject.ownerNickname(),
            dataObject.ownerMobile()
        );
    }

    public AdminPetResponse toResponse(AdminPetEntity entity) {
        PetProfileEntity petProfile = entity.getPetProfile();
        AdminPetFamilyResponse family = petProfile.getFamilyId() == null ? null : new AdminPetFamilyResponse(
            String.valueOf(petProfile.getFamilyId()),
            entity.getFamilyName(),
            entity.getFamilyStatus(),
            entity.getFamilyMemberCount()
        );
        return new AdminPetResponse(
            petEntityConverter.toPetDetailResponse(petProfile),
            adminContextConverter.toUserResponse(new AdminUserContextEntity(
                petProfile.getOwnerUserId(),
                entity.getOwnerNickname(),
                entity.getOwnerMobile()
            )),
            family
        );
    }
}
