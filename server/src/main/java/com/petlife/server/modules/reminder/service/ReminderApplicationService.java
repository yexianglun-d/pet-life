package com.petlife.server.modules.reminder.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.reminder.converter.ReminderEntityConverter;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.dto.request.CreateReminderRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.reminder.persistence.command.CreateReminderCommand;
import com.petlife.server.modules.reminder.persistence.command.HandleReminderCommand;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 提醒应用服务。
 *
 * <p>提醒模块承担首页待办、宠物主页待办和后续通知中心的数据出口，因此在应用层统一封装
 * 创建、查询和完成动作，保证后续扩展消息发送逻辑时不侵入控制器。</p>
 */
@Service
public class ReminderApplicationService {

    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final ReminderEntityConverter reminderEntityConverter;

    public ReminderApplicationService(
        ReminderPersistenceMapper reminderPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        ReminderEntityConverter reminderEntityConverter
    ) {
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.reminderEntityConverter = reminderEntityConverter;
    }

    public List<ReminderResponse> listReminders(Long petId) {
        requireAccessiblePet(petId);
        return reminderPersistenceMapper.listRemindersByPetId(petId).stream()
            .map(reminderEntityConverter::toEntity)
            .map(reminderEntityConverter::toResponse)
            .toList();
    }

    @Transactional
    public ReminderResponse createReminder(Long petId, CreateReminderRequest request) {
        requireAccessiblePet(petId);
        CreateReminderCommand command = buildCreateReminderCommand(
            petId,
            request.reminderType(),
            request.title(),
            request.reminderMode(),
            request.cycleValue(),
            request.cycleUnit(),
            DateTimeConverters.toLocalDateTime(request.dueAt(), LocalDateTime.now().plusDays(1))
        );
        reminderPersistenceMapper.insertReminder(command);
        ReminderEntity reminder = reminderEntityConverter.toEntity(reminderPersistenceMapper.findReminderById(command.getId()));
        return reminderEntityConverter.toResponse(reminder);
    }

    @Transactional
    public ReminderResponse completeReminder(Long petId, Long reminderId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        ReminderEntity reminder = requireReminder(petId, reminderId);
        HandleReminderCommand command = new HandleReminderCommand();
        command.setPetId(petId);
        command.setReminderId(reminderId);
        command.setHandledByUserId(currentUserId);
        int updatedRows = reminderPersistenceMapper.completeReminder(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前提醒已处理，不能重复完成");
        }
        createNextReminderIfCycle(reminder);
        return reminderEntityConverter.toResponse(requireReminder(petId, reminderId));
    }

    @Transactional
    public ReminderResponse skipReminder(Long petId, Long reminderId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        ReminderEntity reminder = requireReminder(petId, reminderId);
        HandleReminderCommand command = new HandleReminderCommand();
        command.setPetId(petId);
        command.setReminderId(reminderId);
        command.setHandledByUserId(currentUserId);
        int updatedRows = reminderPersistenceMapper.skipReminder(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前提醒已处理，不能重复跳过");
        }
        createNextReminderIfCycle(reminder);
        return reminderEntityConverter.toResponse(requireReminder(petId, reminderId));
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private ReminderEntity requireReminder(Long petId, Long reminderId) {
        ReminderEntity reminder = reminderEntityConverter.toEntity(
            reminderPersistenceMapper.findReminderByPetIdAndId(petId, reminderId)
        );
        if (reminder == null) {
            throw new BusinessException(ResponseCode.REMINDER_NOT_FOUND);
        }
        return reminder;
    }

    /**
     * 周期提醒处理后不直接复制“当前时间 + 1 个周期”，而是从原计划时间持续向后推算，
     * 直到得到一条真正位于当前时刻之后的下一次提醒，避免用户晚处理后生成立即过期的脏待办。
     */
    private void createNextReminderIfCycle(ReminderEntity reminder) {
        if (!"cycle".equals(reminder.getReminderMode())) {
            return;
        }

        LocalDateTime nextDueAt = calculateNextDueAt(reminder);
        CreateReminderCommand command = buildCreateReminderCommand(
            reminder.getPetId(),
            reminder.getReminderType(),
            reminder.getTitle(),
            reminder.getReminderMode(),
            reminder.getCycleValue(),
            reminder.getCycleUnit(),
            nextDueAt
        );
        reminderPersistenceMapper.insertReminder(command);
    }

    private CreateReminderCommand buildCreateReminderCommand(
        Long petId,
        String reminderType,
        String title,
        String reminderMode,
        Integer cycleValue,
        String cycleUnit,
        LocalDateTime dueAt
    ) {
        String normalizedReminderMode = normalizeReminderMode(reminderMode);
        Integer normalizedCycleValue = null;
        String normalizedCycleUnit = null;
        if ("cycle".equals(normalizedReminderMode)) {
            normalizedCycleValue = normalizeCycleValue(cycleValue);
            normalizedCycleUnit = normalizeCycleUnit(cycleUnit);
        }

        CreateReminderCommand command = new CreateReminderCommand();
        command.setPetId(petId);
        command.setReminderType(reminderType == null ? null : reminderType.trim());
        command.setTitle(title == null ? null : title.trim());
        command.setReminderMode(normalizedReminderMode);
        command.setCycleValue(normalizedCycleValue);
        command.setCycleUnit(normalizedCycleUnit);
        command.setDueAt(dueAt);
        return command;
    }

    private String normalizeReminderMode(String reminderMode) {
        String normalizedReminderMode = reminderMode == null ? "single" : reminderMode.trim();
        if ("single".equals(normalizedReminderMode) || normalizedReminderMode.isEmpty()) {
            return "single";
        }
        if ("cycle".equals(normalizedReminderMode)) {
            return "cycle";
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "提醒模式仅支持 single 或 cycle");
    }

    private Integer normalizeCycleValue(Integer cycleValue) {
        if (cycleValue == null || cycleValue <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "周期提醒必须提供大于 0 的间隔值");
        }
        return cycleValue;
    }

    private String normalizeCycleUnit(String cycleUnit) {
        String normalizedCycleUnit = cycleUnit == null ? "" : cycleUnit.trim();
        if ("day".equals(normalizedCycleUnit) || "week".equals(normalizedCycleUnit) || "month".equals(normalizedCycleUnit)) {
            return normalizedCycleUnit;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "周期单位仅支持 day、week 或 month");
    }

    private LocalDateTime calculateNextDueAt(ReminderEntity reminder) {
        LocalDateTime nextDueAt = reminder.getDueAt();
        LocalDateTime referenceTime = LocalDateTime.now();
        do {
            nextDueAt = advanceDueAt(nextDueAt, reminder.getCycleValue(), reminder.getCycleUnit());
        } while (!nextDueAt.isAfter(referenceTime));
        return nextDueAt;
    }

    private LocalDateTime advanceDueAt(LocalDateTime dueAt, Integer cycleValue, String cycleUnit) {
        if ("day".equals(cycleUnit)) {
            return dueAt.plusDays(cycleValue);
        }
        if ("week".equals(cycleUnit)) {
            return dueAt.plusWeeks(cycleValue);
        }
        return dueAt.plusMonths(cycleValue);
    }

}
