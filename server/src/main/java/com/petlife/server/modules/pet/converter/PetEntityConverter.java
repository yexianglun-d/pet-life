package com.petlife.server.modules.pet.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.persistence.dataobject.PetProfileDataObject;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 宠物域实体转换器。
 */
@Component
public class PetEntityConverter {

    public PetProfileEntity toEntity(PetProfileDataObject petProfileDataObject) {
        if (petProfileDataObject == null) {
            return null;
        }

        return new PetProfileEntity(
            petProfileDataObject.petId(),
            petProfileDataObject.familyId(),
            petProfileDataObject.ownerUserId(),
            petProfileDataObject.petName(),
            petProfileDataObject.petType(),
            petProfileDataObject.breed(),
            petProfileDataObject.gender(),
            petProfileDataObject.birthday(),
            petProfileDataObject.adoptDate(),
            petProfileDataObject.neuterStatus(),
            petProfileDataObject.avatarUrl(),
            petProfileDataObject.weightKg(),
            petProfileDataObject.allergyNotes(),
            petProfileDataObject.medicalHistory(),
            petProfileDataObject.status(),
            petProfileDataObject.createdAt(),
            petProfileDataObject.updatedAt()
        );
    }

    public PetDetailResponse toPetDetailResponse(PetProfileEntity petProfile) {
        return new PetDetailResponse(
            String.valueOf(petProfile.getPetId()),
            petProfile.getPetName(),
            petProfile.getPetType(),
            petProfile.getBreed(),
            petProfile.getGender(),
            petProfile.getBirthday(),
            petProfile.getAdoptDate(),
            toNeuterStatusLabel(petProfile.getNeuterStatus()),
            petProfile.getAvatarUrl(),
            petProfile.getWeightKg() == null ? null : petProfile.getWeightKg().stripTrailingZeros().toPlainString(),
            petProfile.getAllergyNotes(),
            petProfile.getMedicalHistory(),
            petProfile.getStatus(),
            DateTimeConverters.toOffsetDateTime(petProfile.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(petProfile.getUpdatedAt())
        );
    }

    /**
     * 统一吸收前端不同来源的绝育状态表达，避免接口层与存储层绑定枚举细节。
     */
    public Integer toNeuterStatusValue(String neuterStatus) {
        if (neuterStatus == null || neuterStatus.isBlank() || "unknown".equals(neuterStatus)) {
            return null;
        }

        return switch (neuterStatus) {
            case "completed", "yes", "true", "1" -> 1;
            default -> 0;
        };
    }

    /**
     * 体重输入来自表单，接口层统一按字符串接收，避免前端与序列化精度绑定。
     */
    public BigDecimal toWeightKg(String weightKg) {
        if (weightKg == null || weightKg.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(weightKg.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String toNeuterStatusLabel(Integer neuterStatus) {
        if (neuterStatus == null) {
            return "unknown";
        }
        return neuterStatus == 1 ? "completed" : "pending";
    }
}
