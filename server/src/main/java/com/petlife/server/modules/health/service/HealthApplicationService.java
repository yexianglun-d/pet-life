package com.petlife.server.modules.health.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.health.persistence.record.HealthRecordPersistenceRecord;
import com.petlife.server.modules.health.dto.request.CreateHealthRecordRequest;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 健康记录应用服务。
 *
 * <p>该服务负责把宠物维度的健康记录请求转换为统一输出格式，后续切换数据库或聚合更多健康事件时，
 * 控制器层不需要感知底层存储变化。</p>
 */
@Service
public class HealthApplicationService {

    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;

    public HealthApplicationService(
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper
    ) {
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
    }

    public List<HealthRecordResponse> listHealthRecords(Long petId) {
        requireAccessiblePet(petId);
        return healthRecordPersistenceMapper.listHealthRecordsByPetId(petId).stream()
            .map(this::toHealthRecordResponse)
            .toList();
    }

    @Transactional
    public HealthRecordResponse createHealthRecord(Long petId, CreateHealthRecordRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        LocalDateTime occurredAt = DateTimeConverters.toLocalDateTime(request.occurredAt(), LocalDateTime.now());
        healthRecordPersistenceMapper.insertHealthRecord(
            petId,
            currentUserId,
            request.recordType(),
            request.title(),
            occurredAt,
            buildResultSummary(request.value(), request.unit()),
            request.notes()
        );
        HealthRecordPersistenceRecord healthRecord =
            healthRecordPersistenceMapper.findHealthRecordById(healthRecordPersistenceMapper.selectLastInsertId());
        return toHealthRecordResponse(healthRecord);
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private HealthRecordResponse toHealthRecordResponse(HealthRecordPersistenceRecord healthRecord) {
        return new HealthRecordResponse(
            String.valueOf(healthRecord.healthRecordId()),
            String.valueOf(healthRecord.petId()),
            healthRecord.recordType(),
            healthRecord.title(),
            null,
            null,
            DateTimeConverters.toOffsetDateTime(healthRecord.occurredAt()),
            healthRecord.notes(),
            DateTimeConverters.toOffsetDateTime(healthRecord.createdAt())
        );
    }

    private String buildResultSummary(String value, String unit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return unit == null || unit.isBlank() ? value : value + " " + unit;
    }
}
