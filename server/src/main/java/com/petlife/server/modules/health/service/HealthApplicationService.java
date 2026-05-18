package com.petlife.server.modules.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.health.converter.AdminHealthRecordConverter;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.health.domain.entity.AdminHealthRecordEntity;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.dto.request.CreateHealthRecordRequest;
import com.petlife.server.modules.health.dto.request.UpdateHealthRecordRequest;
import com.petlife.server.modules.health.dto.response.AdminHealthRecordResponse;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.health.persistence.command.CreateHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.DeleteHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.UpdateHealthRecordCommand;
import com.petlife.server.modules.media.service.MediaAssetApplicationService;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.reminder.persistence.command.CreateReminderCommand;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

    private static final Set<String> AUTO_REMINDER_RECORD_TYPES = Set.of("vaccine", "deworming", "examination");
    private static final Set<String> HEALTH_ATTACHMENT_MEDIA_TYPES = Set.of("image", "file");

    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final AdminHealthRecordConverter adminHealthRecordConverter;
    private final TimelineApplicationService timelineApplicationService;
    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final MediaAssetApplicationService mediaAssetApplicationService;
    private final ObjectMapper objectMapper;

    public HealthApplicationService(
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        HealthRecordEntityConverter healthRecordEntityConverter,
        AdminHealthRecordConverter adminHealthRecordConverter,
        TimelineApplicationService timelineApplicationService,
        ReminderPersistenceMapper reminderPersistenceMapper,
        MediaAssetApplicationService mediaAssetApplicationService,
        ObjectMapper objectMapper
    ) {
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.adminHealthRecordConverter = adminHealthRecordConverter;
        this.timelineApplicationService = timelineApplicationService;
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.mediaAssetApplicationService = mediaAssetApplicationService;
        this.objectMapper = objectMapper;
    }

    public List<HealthRecordResponse> listHealthRecords(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        return healthRecordPersistenceMapper.listHealthRecordsByPetId(petId).stream()
            .map(healthRecordEntityConverter::toEntity)
            .map(healthRecord -> toResponse(currentUserId, healthRecord))
            .toList();
    }

    public HealthRecordResponse getHealthRecord(Long petId, Long healthRecordId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        return toResponse(currentUserId, requireHealthRecord(petId, healthRecordId));
    }

    public List<AdminHealthRecordResponse> listAdminHealthRecords(
        String recordType,
        Long petId,
        Long operatorUserId,
        String keyword
    ) {
        return healthRecordPersistenceMapper.listAdminHealthRecords(
                normalizeNullableText(recordType),
                petId,
                operatorUserId,
                normalizeAdminKeyword(keyword)
            )
            .stream()
            .map(adminHealthRecordConverter::toEntity)
            .map(this::toAdminResponse)
            .toList();
    }

    public AdminHealthRecordResponse getAdminHealthRecord(Long healthRecordId) {
        AdminHealthRecordEntity healthRecord = adminHealthRecordConverter.toEntity(
            healthRecordPersistenceMapper.findAdminHealthRecordById(healthRecordId)
        );
        if (healthRecord == null) {
            throw new BusinessException(ResponseCode.HEALTH_RECORD_NOT_FOUND);
        }
        return toAdminResponse(healthRecord);
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
        command.setHospitalName(normalizeNullableText(request.hospitalName()));
        command.setDoctorName(normalizeNullableText(request.doctorName()));
        command.setSeverityLevel(normalizeNullableText(request.severityLevel()));
        command.setResultSummary(buildResultSummary(request.resultSummary(), request.value(), request.unit()));
        command.setAttachmentsJson(toAttachmentAssetIdsJson(
            mediaAssetApplicationService.validateUsableAssetIds(
                currentUserId,
                request.attachmentAssetIds(),
                "health_report",
                HEALTH_ATTACHMENT_MEDIA_TYPES
            )
        ));
        command.setNotes(normalizeNullableText(request.notes()));
        healthRecordPersistenceMapper.insertHealthRecord(command);
        syncNextReminder(petId, command.getId(), request.recordType(), request.title(), request.nextReminderAt(), request.nextReminderTitle());
        HealthRecordEntity healthRecord = requireHealthRecord(petId, command.getId());
        timelineApplicationService.syncHealthRecordEvent(healthRecord);
        return toResponse(currentUserId, healthRecord);
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
        command.setHospitalName(normalizeNullableText(request.hospitalName()));
        command.setDoctorName(normalizeNullableText(request.doctorName()));
        command.setSeverityLevel(normalizeNullableText(request.severityLevel()));
        command.setResultSummary(buildResultSummary(request.resultSummary(), request.value(), request.unit()));
        command.setAttachmentsJson(toAttachmentAssetIdsJson(
            mediaAssetApplicationService.validateUsableAssetIds(
                currentUserId,
                request.attachmentAssetIds(),
                "health_report",
                HEALTH_ATTACHMENT_MEDIA_TYPES
            )
        ));
        command.setNotes(normalizeNullableText(request.notes()));

        int updatedRows = healthRecordPersistenceMapper.updateHealthRecord(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.HEALTH_RECORD_NOT_FOUND);
        }

        syncNextReminder(petId, healthRecordId, request.recordType(), request.title(), request.nextReminderAt(), request.nextReminderTitle());
        HealthRecordEntity healthRecord = requireHealthRecord(petId, healthRecordId);
        timelineApplicationService.syncHealthRecordEvent(healthRecord);
        return toResponse(currentUserId, healthRecord);
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
        reminderPersistenceMapper.deletePendingRemindersBySourceRecordId(healthRecordId);
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

    private HealthRecordResponse toResponse(Long currentUserId, HealthRecordEntity healthRecord) {
        return healthRecordEntityConverter.toResponse(
            healthRecord,
            mediaAssetApplicationService.listReadableMediaAssetResponses(
                currentUserId,
                healthRecord.getAttachmentAssetIds()
            )
        );
    }

    private AdminHealthRecordResponse toAdminResponse(AdminHealthRecordEntity healthRecord) {
        return adminHealthRecordConverter.toResponse(
            healthRecord,
            mediaAssetApplicationService.listUploadedMediaAssetResponses(
                healthRecord.getHealthRecord().getAttachmentAssetIds()
            )
        );
    }

    /**
     * 健康记录的“下一次提醒”必须从健康事实派生，且只允许疫苗、驱虫、体检这类天然有复查周期的记录生成。
     * 更新健康记录时先删除仍未处理的旧派生提醒，再按最新表单重建，避免同一健康记录堆出多个待办。
     */
    private void syncNextReminder(
        Long petId,
        Long healthRecordId,
        String recordType,
        String title,
        java.time.OffsetDateTime nextReminderAt,
        String nextReminderTitle
    ) {
        reminderPersistenceMapper.deletePendingRemindersBySourceRecordId(healthRecordId);
        if (nextReminderAt == null) {
            return;
        }

        String normalizedRecordType = normalizeNullableText(recordType);
        if (!AUTO_REMINDER_RECORD_TYPES.contains(normalizedRecordType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "只有疫苗、驱虫、体检记录支持自动生成下一次提醒");
        }

        LocalDateTime dueAt = DateTimeConverters.toLocalDateTime(nextReminderAt, null);
        if (dueAt == null || !dueAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "下一次提醒时间必须晚于当前时间");
        }

        CreateReminderCommand reminderCommand = new CreateReminderCommand();
        reminderCommand.setPetId(petId);
        reminderCommand.setReminderType(toReminderType(normalizedRecordType));
        reminderCommand.setTitle(buildNextReminderTitle(title, nextReminderTitle));
        reminderCommand.setReminderMode("single");
        reminderCommand.setDueAt(dueAt);
        reminderCommand.setSourceRecordId(healthRecordId);
        reminderPersistenceMapper.insertReminder(reminderCommand);
    }

    private String buildResultSummary(String resultSummary, String value, String unit) {
        String normalizedResultSummary = normalizeNullableText(resultSummary);
        if (normalizedResultSummary != null) {
            return normalizedResultSummary;
        }
        String normalizedValue = normalizeNullableText(value);
        if (normalizedValue == null) {
            return null;
        }
        String normalizedUnit = normalizeNullableText(unit);
        return normalizedUnit == null ? normalizedValue : normalizedValue + " " + normalizedUnit;
    }

    private String buildNextReminderTitle(String title, String nextReminderTitle) {
        String normalizedNextReminderTitle = normalizeNullableText(nextReminderTitle);
        if (normalizedNextReminderTitle != null) {
            return normalizedNextReminderTitle;
        }
        return "下次" + normalizeNullableText(title);
    }

    private String toReminderType(String recordType) {
        if ("examination".equals(recordType)) {
            return "checkup";
        }
        return recordType;
    }

    private String toAttachmentAssetIdsJson(List<String> attachmentAssetIds) {
        if (attachmentAssetIds == null || attachmentAssetIds.isEmpty()) {
            return "[]";
        }
        List<String> normalizedAttachmentAssetIds = attachmentAssetIds.stream()
            .map(this::normalizeNullableText)
            .filter(assetId -> assetId != null)
            .distinct()
            .toList();
        try {
            return objectMapper.writeValueAsString(normalizedAttachmentAssetIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "附件信息格式不正确");
        }
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        return normalizedText.isEmpty() ? null : normalizedText;
    }

    private String normalizeAdminKeyword(String keyword) {
        String normalizedKeyword = normalizeNullableText(keyword);
        if (normalizedKeyword == null) {
            return null;
        }
        return normalizedKeyword.length() <= 100 ? normalizedKeyword : normalizedKeyword.substring(0, 100);
    }
}
