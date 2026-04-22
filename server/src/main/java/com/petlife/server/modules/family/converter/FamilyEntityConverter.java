package com.petlife.server.modules.family.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.family.domain.entity.FamilyInvitationEntity;
import com.petlife.server.modules.family.domain.entity.FamilyMemberEntity;
import com.petlife.server.modules.family.domain.entity.FamilyProfileEntity;
import com.petlife.server.modules.family.dto.response.FamilyDetailResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationPreviewResponse;
import com.petlife.server.modules.family.dto.response.FamilyMemberResponse;
import com.petlife.server.modules.family.dto.response.FamilySharedPetResponse;
import com.petlife.server.modules.family.persistence.dataobject.FamilyInvitationDataObject;
import com.petlife.server.modules.family.persistence.dataobject.FamilyMemberDataObject;
import com.petlife.server.modules.family.persistence.dataobject.FamilyProfileDataObject;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 家庭领域实体转换器。
 */
@Component
public class FamilyEntityConverter {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public FamilyEntityConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FamilyProfileEntity toEntity(FamilyProfileDataObject familyProfileDataObject) {
        if (familyProfileDataObject == null) {
            return null;
        }

        return new FamilyProfileEntity(
            familyProfileDataObject.familyId(),
            familyProfileDataObject.familyName(),
            familyProfileDataObject.ownerUserId(),
            familyProfileDataObject.memberCount(),
            familyProfileDataObject.currentUserRole()
        );
    }

    public FamilyMemberEntity toEntity(FamilyMemberDataObject familyMemberDataObject) {
        if (familyMemberDataObject == null) {
            return null;
        }

        return new FamilyMemberEntity(
            familyMemberDataObject.memberId(),
            familyMemberDataObject.familyId(),
            familyMemberDataObject.userId(),
            familyMemberDataObject.nickname(),
            familyMemberDataObject.mobile(),
            familyMemberDataObject.role(),
            familyMemberDataObject.inviteStatus(),
            familyMemberDataObject.joinedAt()
        );
    }

    public FamilyInvitationEntity toEntity(FamilyInvitationDataObject familyInvitationDataObject) {
        if (familyInvitationDataObject == null) {
            return null;
        }

        return new FamilyInvitationEntity(
            familyInvitationDataObject.invitationId(),
            familyInvitationDataObject.familyId(),
            familyInvitationDataObject.inviterUserId(),
            familyInvitationDataObject.inviteeMobile(),
            familyInvitationDataObject.inviteeUserId(),
            familyInvitationDataObject.role(),
            fromJson(familyInvitationDataObject.sharedPetIdsJson()),
            familyInvitationDataObject.inviteCode(),
            familyInvitationDataObject.status(),
            familyInvitationDataObject.expiredAt(),
            familyInvitationDataObject.acceptedAt(),
            familyInvitationDataObject.createdAt()
        );
    }

    public FamilyDetailResponse toDetailResponse(
        FamilyProfileEntity familyProfile,
        List<FamilyMemberResponse> members,
        List<FamilySharedPetResponse> sharedPets,
        List<FamilyInvitationResponse> pendingInvitations
    ) {
        return new FamilyDetailResponse(
            String.valueOf(familyProfile.getFamilyId()),
            familyProfile.getFamilyName(),
            familyProfile.getMemberCount(),
            familyProfile.getCurrentUserRole(),
            members,
            sharedPets,
            pendingInvitations
        );
    }

    public FamilyMemberResponse toMemberResponse(FamilyMemberEntity familyMember) {
        return new FamilyMemberResponse(
            String.valueOf(familyMember.getMemberId()),
            String.valueOf(familyMember.getUserId()),
            familyMember.getNickname(),
            familyMember.getMobile(),
            familyMember.getRole(),
            familyMember.getInviteStatus(),
            DateTimeConverters.toOffsetDateTime(familyMember.getJoinedAt())
        );
    }

    public FamilyInvitationResponse toInvitationResponse(FamilyInvitationEntity familyInvitation) {
        return new FamilyInvitationResponse(
            String.valueOf(familyInvitation.getInvitationId()),
            familyInvitation.getInviteeMobile(),
            familyInvitation.getRole(),
            familyInvitation.getSharedPetIds().stream().map(String::valueOf).toList(),
            familyInvitation.getInviteCode(),
            familyInvitation.getStatus(),
            DateTimeConverters.toOffsetDateTime(familyInvitation.getExpiredAt()),
            DateTimeConverters.toOffsetDateTime(familyInvitation.getCreatedAt())
        );
    }

    public FamilySharedPetResponse toSharedPetResponse(PetDetailResponse petDetail) {
        return new FamilySharedPetResponse(
            petDetail.petId(),
            petDetail.petName(),
            petDetail.petType(),
            petDetail.breed()
        );
    }

    public FamilyInvitationPreviewResponse toInvitationPreviewResponse(
        FamilyInvitationEntity familyInvitation,
        FamilyProfileEntity familyProfile,
        String inviterNickname,
        List<FamilySharedPetResponse> sharedPets
    ) {
        return new FamilyInvitationPreviewResponse(
            String.valueOf(familyInvitation.getInvitationId()),
            String.valueOf(familyProfile.getFamilyId()),
            familyProfile.getFamilyName(),
            inviterNickname,
            familyInvitation.getInviteeMobile(),
            familyInvitation.getRole(),
            sharedPets,
            familyInvitation.getInviteCode(),
            familyInvitation.getStatus(),
            DateTimeConverters.toOffsetDateTime(familyInvitation.getExpiredAt()),
            DateTimeConverters.toOffsetDateTime(familyInvitation.getCreatedAt())
        );
    }

    /**
     * 邀请共享宠物配置先以 JSON 数组落库，保证前后端只有一处序列化规则。
     */
    public String toSharedPetIdsJson(List<Long> sharedPetIds) {
        try {
            return objectMapper.writeValueAsString(sharedPetIds == null ? List.of() : sharedPetIds);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "共享宠物配置格式不合法");
        }
    }

    private List<Long> fromJson(String sharedPetIdsJson) {
        if (sharedPetIdsJson == null || sharedPetIdsJson.isBlank()) {
            return List.of();
        }

        try {
            return List.copyOf(objectMapper.readValue(sharedPetIdsJson, LONG_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("family_invitations.shared_pet_ids 数据格式不合法", ex);
        }
    }
}
