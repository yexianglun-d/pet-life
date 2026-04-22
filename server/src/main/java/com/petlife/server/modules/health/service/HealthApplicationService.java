package com.petlife.server.modules.health.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.dto.request.CreateHealthRecordRequest;
import com.petlife.server.modules.health.dto.request.UpdateHealthRecordRequest;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.health.persistence.command.CreateHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.DeleteHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.UpdateHealthRecordCommand;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
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
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final TimelineApplicationService timelineApplicationService;

    public HealthApplicationService(
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        HealthRecordEntityConverter healthRecordEntityConverter,
        TimelineApplicationService timelineApplicationService
    ) {
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.timelineApplicationService = timelineApplicationService;
    }

    public List<HealthRecordResponse> listHealthRecords(Long petId) {
        requireAccessiblePet(petId);
        return healthRecordPersistenceMapper.listHealthRecordsByPetId(petId).stream()
            .map(healthRecordEntityConverter::toEntity)
            .map(healthRecordEntityConverter::toResponse)
            .toList();
    }

    public HealthRecordResponse getHealthRecord(Long petId, Long healthRecordId) {
        requireAccessiblePet(petId);
        return healthRecordEntityConverter.toResponse(requireHealthRecord(petId, healthRecordId));
    }

    @Transactional
    public HealthRecordResponse createHealthRecord(Long petId, CreateHealthRecordRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        CreateHealthRecordCommand command = new CreateHealthRecordCommand();
        command.setPetId(petId);
        command.setOperatorUserId(currentUserId);
        command.setRecordType(request.recordType().trim());
        command.setTitle(request.title().trim());
        command.setOccurredAt(DateTimeConverters.toLocalDateTime(request.occurredAt(), LocalDateTime.now()));
        command.setResultSummary(buildResultSummary(request.value(), request.unit()));
        command.setNotes(normalizeNullableText(request.notes()));
        healthRecordPersistenceMapper.insertHealthRecord(command);
        HealthRecordEntity healthRecord = healthRecordEntityConverter.toEntity(
            healthRecordPersistenceMapper.findHealthRecordById(command.getId())
        );
        timelineApplicationService.syncHealthRecordEvent(healthRecord);
        return healthRecordEntityConverter.toResponse(healthRecord);
    }

    @Transactional
    public HealthRecordResponse updateHealthRecord(
        Long petId,
        Long healthRecordId,
        UpdateHealthRecordRequest request
    ) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        requireHealthRecord(petId, healthRecordId);

        UpdateHealthRecordCommand command = new UpdateHealthRecordCommand();
        command.setHealthRecordId(healthRecordId);
        command.setPetId(petId);
        command.setOperatorUserId(currentUserId);
        command.setRecordType(request.recordType().trim());
        command.setTitle(request.title().trim());
        command.setOccurredAt(DateTimeConverters.toLocalDateTime(request.occurredAt(), LocalDateTime.now()));
        command.setResultSummary(buildResultSummary(request.value(), request.unit()));
        command.setNotes(normalizeNullableText(request.notes()));

        int updatedRows = healthRecordPersistenceMapper.updateHealthRecord(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.HEALTH_RECORD_NOT_FOUND);
        }

        HealthRecordEntity healthRecord = requireHealthRecord(petId, healthRecordId);
        timelineApplicationService.syncHealthRecordEvent(healthRecord);
        return healthRecordEntityConverter.toResponse(healthRecord);
    }

    @Transactional
    public void deleteHealthRecord(Long petId, Long healthRecordId) {
        requireAccessiblePet(petId);
        requireHealthRecord(petId, healthRecordId);

        DeleteHealthRecordCommand command = new DeleteHealthRecordCommand();
        command.setPetId(petId);
        command.setHealthRecordId(healthRecordId);
        int deletedRows = healthRecordPersistenceMapper.deleteHealthRecord(command);
        if (deletedRows == 0) {
            throw new BusinessException(ResponseCode.HEALTH_RECORD_NOT_FOUND);
        }
        timelineApplicationService.deleteHealthRecordEvent(petId, healthRecordId);
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private HealthRecordEntity requireHealthRecord(Long petId, Long healthRecordId) {
        HealthRecordEntity healthRecord = healthRecordEntityConverter.toEntity(
            healthRecordPersistenceMapper.findHealthRecordByPetIdAndId(petId, healthRecordId)
        );
        if (healthRecord == null) {
            throw new BusinessException(ResponseCode.HEALTH_RECORD_NOT_FOUND);
        }
        return healthRecord;
    }

    private String buildResultSummary(String value, String unit) {
        String normalizedValue = normalizeNullableText(value);
        if (normalizedValue == null) {
            return null;
        }
        String normalizedUnit = normalizeNullableText(unit);
        return normalizedUnit == null ? normalizedValue : normalizedValue + " " + normalizedUnit;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        return normalizedText.isEmpty() ? null : normalizedText;
    }
}
