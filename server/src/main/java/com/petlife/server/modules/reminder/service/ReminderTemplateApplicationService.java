package com.petlife.server.modules.reminder.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.dataobject.PetProfileDataObject;
import com.petlife.server.modules.reminder.converter.ReminderTemplateConverter;
import com.petlife.server.modules.reminder.domain.entity.ReminderTemplateEntity;
import com.petlife.server.modules.reminder.dto.request.AdminUpdateReminderTemplateStatusRequest;
import com.petlife.server.modules.reminder.dto.request.AdminUpsertReminderTemplateRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderTemplateResponse;
import com.petlife.server.modules.reminder.persistence.ReminderTemplatePersistenceMapper;
import com.petlife.server.modules.reminder.persistence.command.UpdateReminderTemplateStatusCommand;
import com.petlife.server.modules.reminder.persistence.command.UpsertReminderTemplateCommand;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台提醒模板应用服务。
 */
@Service
public class ReminderTemplateApplicationService {

    private static final Set<String> SUPPORTED_REMINDER_TYPES = Set.of(
        "vaccine",
        "deworming",
        "examination",
        "medication",
        "custom"
    );
    private static final Set<String> SUPPORTED_REMINDER_MODES = Set.of("single", "cycle");
    private static final Set<String> SUPPORTED_UNITS = Set.of("day", "week", "month");
    private static final Set<String> SUPPORTED_PET_TYPES = Set.of("all", "cat", "dog", "other");
    private static final String AUDIT_TARGET_REMINDER_TEMPLATE = "reminder_template";
    private static final String AUDIT_ACTION_CREATE = "reminder_template_create";
    private static final String AUDIT_ACTION_UPDATE = "reminder_template_update";
    private static final String AUDIT_ACTION_STATUS_UPDATE = "reminder_template_status_update";

    private final ReminderTemplatePersistenceMapper reminderTemplatePersistenceMapper;
    private final ReminderTemplateConverter reminderTemplateConverter;
    private final AuditLogApplicationService auditLogApplicationService;
    private final PetPersistenceMapper petPersistenceMapper;

    public ReminderTemplateApplicationService(
        ReminderTemplatePersistenceMapper reminderTemplatePersistenceMapper,
        ReminderTemplateConverter reminderTemplateConverter,
        AuditLogApplicationService auditLogApplicationService,
        PetPersistenceMapper petPersistenceMapper
    ) {
        this.reminderTemplatePersistenceMapper = reminderTemplatePersistenceMapper;
        this.reminderTemplateConverter = reminderTemplateConverter;
        this.auditLogApplicationService = auditLogApplicationService;
        this.petPersistenceMapper = petPersistenceMapper;
    }

    public List<ReminderTemplateResponse> listUserTemplates(Long petId) {
        PetProfileDataObject pet = requireAccessiblePet(petId);
        return reminderTemplatePersistenceMapper
            .listEnabledTemplatesForPetType(pet.petType())
            .stream()
            .map(reminderTemplateConverter::toEntity)
            .map(reminderTemplateConverter::toResponse)
            .toList();
    }

    public List<ReminderTemplateResponse> listAdminTemplates(
        String keyword,
        String reminderType,
        String defaultReminderMode,
        String applicablePetType,
        Boolean enabled
    ) {
        String normalizedKeyword = normalizeOptionalText(keyword, 100, "搜索关键词长度不能超过 100 个字符");
        String normalizedReminderType = normalizeOptionalReminderType(reminderType);
        String normalizedDefaultReminderMode = normalizeOptionalReminderMode(defaultReminderMode);
        String normalizedApplicablePetType = normalizeOptionalPetType(applicablePetType);
        return reminderTemplatePersistenceMapper
            .listAdminTemplates(
                normalizedKeyword,
                normalizedReminderType,
                normalizedDefaultReminderMode,
                normalizedApplicablePetType,
                enabled
            )
            .stream()
            .map(reminderTemplateConverter::toEntity)
            .map(reminderTemplateConverter::toResponse)
            .toList();
    }

    public ReminderTemplateResponse getAdminTemplate(Long templateId) {
        return reminderTemplateConverter.toResponse(requireTemplate(templateId));
    }

    @Transactional
    public ReminderTemplateResponse createAdminTemplate(
        AdminUpsertReminderTemplateRequest request,
        AdminOperationContext operationContext
    ) {
        UpsertReminderTemplateCommand command = buildUpsertCommand(null, request);
        reminderTemplatePersistenceMapper.insertTemplate(command);
        ReminderTemplateEntity template = requireTemplate(command.getTemplateId());
        auditTemplateOperation(operationContext, template, AUDIT_ACTION_CREATE);
        return reminderTemplateConverter.toResponse(template);
    }

    @Transactional
    public ReminderTemplateResponse updateAdminTemplate(
        Long templateId,
        AdminUpsertReminderTemplateRequest request,
        AdminOperationContext operationContext
    ) {
        requireTemplate(templateId);
        UpsertReminderTemplateCommand command = buildUpsertCommand(templateId, request);
        int updatedRows = reminderTemplatePersistenceMapper.updateTemplate(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.REMINDER_TEMPLATE_NOT_FOUND);
        }
        ReminderTemplateEntity template = requireTemplate(templateId);
        auditTemplateOperation(operationContext, template, AUDIT_ACTION_UPDATE);
        return reminderTemplateConverter.toResponse(template);
    }

    @Transactional
    public ReminderTemplateResponse updateAdminTemplateStatus(
        Long templateId,
        AdminUpdateReminderTemplateStatusRequest request,
        AdminOperationContext operationContext
    ) {
        requireTemplate(templateId);
        UpdateReminderTemplateStatusCommand command = new UpdateReminderTemplateStatusCommand();
        command.setTemplateId(templateId);
        command.setEnabled(Boolean.TRUE.equals(request.enabled()));
        int updatedRows = reminderTemplatePersistenceMapper.updateTemplateStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.REMINDER_TEMPLATE_NOT_FOUND);
        }
        ReminderTemplateEntity template = requireTemplate(templateId);
        auditTemplateOperation(operationContext, template, AUDIT_ACTION_STATUS_UPDATE);
        return reminderTemplateConverter.toResponse(template);
    }

    private ReminderTemplateEntity requireTemplate(Long templateId) {
        ReminderTemplateEntity template = reminderTemplateConverter.toEntity(
            reminderTemplatePersistenceMapper.findAdminTemplateById(templateId)
        );
        if (template == null) {
            throw new BusinessException(ResponseCode.REMINDER_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private PetProfileDataObject requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        PetProfileDataObject pet = petPersistenceMapper.findAccessiblePetById(currentUserId, petId);
        if (pet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return pet;
    }

    private UpsertReminderTemplateCommand buildUpsertCommand(
        Long templateId,
        AdminUpsertReminderTemplateRequest request
    ) {
        String normalizedReminderMode = normalizeRequiredReminderMode(request.defaultReminderMode());
        Integer normalizedCycleValue = normalizeCycleValue(normalizedReminderMode, request.defaultCycleValue());
        String normalizedCycleUnit = normalizeCycleUnit(normalizedReminderMode, request.defaultCycleUnit());

        UpsertReminderTemplateCommand command = new UpsertReminderTemplateCommand();
        command.setTemplateId(templateId);
        command.setTemplateName(normalizeRequiredText(request.templateName(), "模板名称不能为空", "模板名称不能超过 100 个字符", 100));
        command.setReminderType(normalizeRequiredReminderType(request.reminderType()));
        command.setDefaultReminderMode(normalizedReminderMode);
        command.setDefaultAdvanceValue(normalizeAdvanceValue(request.defaultAdvanceValue()));
        command.setDefaultAdvanceUnit(normalizeUnit(request.defaultAdvanceUnit(), "默认提前单位仅支持 day、week 或 month"));
        command.setDefaultCycleValue(normalizedCycleValue);
        command.setDefaultCycleUnit(normalizedCycleUnit);
        command.setApplicablePetType(normalizeRequiredPetType(request.applicablePetType()));
        command.setEnabled(Boolean.TRUE.equals(request.enabled()));
        command.setSortOrder(normalizeSortOrder(request.sortOrder()));
        return command;
    }

    private Integer normalizeCycleValue(String reminderMode, Integer cycleValue) {
        if ("single".equals(reminderMode)) {
            if (cycleValue != null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "单次提醒模板不能配置默认周期值");
            }
            return null;
        }
        if (cycleValue == null || cycleValue <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "周期提醒模板必须提供大于 0 的默认周期值");
        }
        return cycleValue;
    }

    private String normalizeCycleUnit(String reminderMode, String cycleUnit) {
        if ("single".equals(reminderMode)) {
            String normalizedCycleUnit = normalizeOptionalText(cycleUnit, 20, "默认周期单位不能超过 20 个字符");
            if (normalizedCycleUnit != null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "单次提醒模板不能配置默认周期单位");
            }
            return null;
        }
        return normalizeUnit(cycleUnit, "默认周期单位仅支持 day、week 或 month");
    }

    private Integer normalizeAdvanceValue(Integer advanceValue) {
        if (advanceValue == null || advanceValue < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "默认提前量不能小于 0");
        }
        return advanceValue;
    }

    private String normalizeRequiredReminderType(String reminderType) {
        String normalizedReminderType = normalizeRequiredText(reminderType, "提醒类型不能为空", "提醒类型长度不能超过 30 个字符", 30);
        if (!SUPPORTED_REMINDER_TYPES.contains(normalizedReminderType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "提醒类型不支持");
        }
        return normalizedReminderType;
    }

    private String normalizeOptionalReminderType(String reminderType) {
        String normalizedReminderType = normalizeOptionalText(reminderType, 30, "提醒类型长度不能超过 30 个字符");
        if (normalizedReminderType == null) {
            return null;
        }
        if (!SUPPORTED_REMINDER_TYPES.contains(normalizedReminderType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "提醒类型不支持");
        }
        return normalizedReminderType;
    }

    private String normalizeRequiredReminderMode(String reminderMode) {
        String normalizedReminderMode = normalizeRequiredText(reminderMode, "默认提醒模式不能为空", "默认提醒模式长度不能超过 20 个字符", 20);
        if (!SUPPORTED_REMINDER_MODES.contains(normalizedReminderMode)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "默认提醒模式仅支持 single 或 cycle");
        }
        return normalizedReminderMode;
    }

    private String normalizeOptionalReminderMode(String reminderMode) {
        String normalizedReminderMode = normalizeOptionalText(reminderMode, 20, "默认提醒模式长度不能超过 20 个字符");
        if (normalizedReminderMode == null) {
            return null;
        }
        if (!SUPPORTED_REMINDER_MODES.contains(normalizedReminderMode)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "默认提醒模式仅支持 single 或 cycle");
        }
        return normalizedReminderMode;
    }

    private String normalizeRequiredPetType(String applicablePetType) {
        String normalizedPetType = normalizeRequiredText(applicablePetType, "适用宠物类型不能为空", "适用宠物类型长度不能超过 20 个字符", 20);
        if (!SUPPORTED_PET_TYPES.contains(normalizedPetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "适用宠物类型仅支持 all、cat、dog 或 other");
        }
        return normalizedPetType;
    }

    private String normalizeOptionalPetType(String applicablePetType) {
        String normalizedPetType = normalizeOptionalText(applicablePetType, 20, "适用宠物类型长度不能超过 20 个字符");
        if (normalizedPetType == null) {
            return null;
        }
        if (!SUPPORTED_PET_TYPES.contains(normalizedPetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "适用宠物类型仅支持 all、cat、dog 或 other");
        }
        return normalizedPetType;
    }

    private String normalizeUnit(String unit, String message) {
        String normalizedUnit = normalizeRequiredText(unit, "时间单位不能为空", "时间单位长度不能超过 20 个字符", 20);
        if (!SUPPORTED_UNITS.contains(normalizedUnit)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, message);
        }
        return normalizedUnit;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }
        if (sortOrder < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "排序值不能小于 0");
        }
        return sortOrder;
    }

    private String normalizeRequiredText(String value, String emptyMessage, String maxLengthMessage, int maxLength) {
        String normalizedValue = normalizeOptionalText(value, maxLength, maxLengthMessage);
        if (normalizedValue == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, emptyMessage);
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value, int maxLength, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, errorMessage);
        }
        return normalizedValue;
    }

    private void auditTemplateOperation(
        AdminOperationContext operationContext,
        ReminderTemplateEntity template,
        String action
    ) {
        // 模板管理是后台配置写操作，沿用既有审计链路记录关键配置快照，便于后续排查配置变更。
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            AUDIT_TARGET_REMINDER_TEMPLATE,
            template.getTemplateId().toString(),
            action,
            Map.of(
                "template_name", template.getTemplateName(),
                "reminder_type", template.getReminderType(),
                "default_reminder_mode", template.getDefaultReminderMode(),
                "applicable_pet_type", template.getApplicablePetType(),
                "enabled", template.isEnabled()
            )
        );
    }
}
