package com.petlife.server.modules.family.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.family.domain.entity.AdminFamilyEntity;
import com.petlife.server.modules.family.domain.entity.AdminFamilyPetEntity;
import com.petlife.server.modules.family.domain.entity.FamilyMemberEntity;
import com.petlife.server.modules.family.dto.response.AdminFamilyPetResponse;
import com.petlife.server.modules.family.dto.response.AdminFamilyResponse;
import com.petlife.server.modules.family.dto.response.FamilyMemberResponse;
import com.petlife.server.modules.family.persistence.dataobject.AdminFamilyDataObject;
import com.petlife.server.modules.family.persistence.dataobject.AdminFamilyPetDataObject;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 后台家庭转换器。
 */
@Component
public class AdminFamilyConverter {

    private final AdminContextConverter adminContextConverter;
    private final FamilyEntityConverter familyEntityConverter;

    public AdminFamilyConverter(
        AdminContextConverter adminContextConverter,
        FamilyEntityConverter familyEntityConverter
    ) {
        this.adminContextConverter = adminContextConverter;
        this.familyEntityConverter = familyEntityConverter;
    }

    public AdminFamilyEntity toEntity(
        AdminFamilyDataObject dataObject,
        List<FamilyMemberEntity> members,
        List<AdminFamilyPetEntity> pets
    ) {
        if (dataObject == null) {
            return null;
        }
        return new AdminFamilyEntity(
            dataObject.familyId(),
            dataObject.familyName(),
            dataObject.ownerUserId(),
            dataObject.ownerNickname(),
            dataObject.ownerMobile(),
            dataObject.status(),
            dataObject.createdAt(),
            dataObject.updatedAt(),
            dataObject.memberCount() == null ? 0 : dataObject.memberCount(),
            dataObject.petCount() == null ? 0 : dataObject.petCount(),
            members == null ? List.of() : List.copyOf(members),
            pets == null ? List.of() : List.copyOf(pets)
        );
    }

    public AdminFamilyPetEntity toPetEntity(AdminFamilyPetDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new AdminFamilyPetEntity(
            dataObject.petId(),
            dataObject.familyId(),
            dataObject.petName(),
            dataObject.petType(),
            dataObject.breed(),
            dataObject.status(),
            dataObject.ownerUserId(),
            dataObject.ownerNickname(),
            dataObject.ownerMobile()
        );
    }

    public AdminFamilyResponse toResponse(AdminFamilyEntity entity) {
        return new AdminFamilyResponse(
            String.valueOf(entity.getFamilyId()),
            entity.getFamilyName(),
            adminContextConverter.toUserResponse(new AdminUserContextEntity(
                entity.getOwnerUserId(),
                entity.getOwnerNickname(),
                entity.getOwnerMobile()
            )),
            entity.getStatus(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt()),
            entity.getMemberCount(),
            entity.getPetCount(),
            entity.getMembers().stream()
                .map(familyEntityConverter::toMemberResponse)
                .toList(),
            entity.getPets().stream()
                .map(this::toPetResponse)
                .toList()
        );
    }

    private AdminFamilyPetResponse toPetResponse(AdminFamilyPetEntity entity) {
        return new AdminFamilyPetResponse(
            String.valueOf(entity.getPetId()),
            entity.getPetName(),
            entity.getPetType(),
            entity.getBreed(),
            entity.getStatus(),
            entity.getOwnerUserId() == null ? null : String.valueOf(entity.getOwnerUserId()),
            entity.getOwnerNickname(),
            entity.getOwnerMobile()
        );
    }
}
