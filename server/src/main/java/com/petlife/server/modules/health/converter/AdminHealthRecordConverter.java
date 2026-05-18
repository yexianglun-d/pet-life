package com.petlife.server.modules.health.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.health.domain.entity.AdminHealthRecordEntity;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.dto.response.AdminHealthRecordResponse;
import com.petlife.server.modules.health.persistence.dataobject.AdminHealthRecordDataObject;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 后台健康记录转换器。
 */
@Component
public class AdminHealthRecordConverter {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final AdminContextConverter adminContextConverter;

    public AdminHealthRecordConverter(
        ObjectMapper objectMapper,
        HealthRecordEntityConverter healthRecordEntityConverter,
        AdminContextConverter adminContextConverter
    ) {
        this.objectMapper = objectMapper;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.adminContextConverter = adminContextConverter;
    }

    public AdminHealthRecordEntity toEntity(AdminHealthRecordDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        HealthRecordEntity healthRecord = new HealthRecordEntity(
            dataObject.healthRecordId(),
            dataObject.petId(),
            dataObject.operatorUserId(),
            dataObject.recordType(),
            dataObject.title(),
            dataObject.occurredAt(),
            dataObject.hospitalName(),
            dataObject.doctorName(),
            dataObject.severityLevel(),
            dataObject.resultSummary(),
            parseAssetIds(dataObject.attachments()),
            dataObject.nextReminderId(),
            dataObject.nextReminderAt(),
            dataObject.nextReminderStatus(),
            dataObject.notes(),
            dataObject.createdAt()
        );
        return new AdminHealthRecordEntity(
            healthRecord,
            new AdminPetContextEntity(
                dataObject.petId(),
                dataObject.petName(),
                dataObject.petType(),
                dataObject.familyId(),
                dataObject.familyName(),
                dataObject.ownerUserId(),
                dataObject.ownerNickname(),
                dataObject.ownerMobile()
            ),
            new AdminUserContextEntity(
                dataObject.operatorUserId(),
                dataObject.operatorNickname(),
                dataObject.operatorMobile()
            )
        );
    }

    public AdminHealthRecordResponse toResponse(
        AdminHealthRecordEntity entity,
        List<MediaAssetResponse> attachmentAssets
    ) {
        return new AdminHealthRecordResponse(
            healthRecordEntityConverter.toResponse(entity.getHealthRecord(), attachmentAssets),
            adminContextConverter.toPetResponse(entity.getPetContext()),
            adminContextConverter.toUserResponse(entity.getOperatorContext())
        );
    }

    private List<String> parseAssetIds(String assetIdsJson) {
        if (assetIdsJson == null || assetIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(assetIdsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
