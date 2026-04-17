package com.petlife.server.modules.reminder.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.reminder.persistence.record.ReminderPersistenceRecord;
import com.petlife.server.modules.reminder.dto.request.CreateReminderRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
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

    public ReminderApplicationService(
        ReminderPersistenceMapper reminderPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper
    ) {
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
    }

    public List<ReminderResponse> listReminders(Long petId) {
        requireAccessiblePet(petId);
        return reminderPersistenceMapper.listRemindersByPetId(petId).stream()
            .map(this::toReminderResponse)
            .toList();
    }

    @Transactional
    public ReminderResponse createReminder(Long petId, CreateReminderRequest request) {
        requireAccessiblePet(petId);
        reminderPersistenceMapper.insertReminder(
            petId,
            request.reminderType(),
            request.title(),
            DateTimeConverters.toLocalDateTime(request.dueAt(), LocalDateTime.now().plusDays(1)),
            request.notes()
        );
        ReminderPersistenceRecord reminder =
            reminderPersistenceMapper.findReminderById(reminderPersistenceMapper.selectLastInsertId());
        return toReminderResponse(reminder);
    }

    @Transactional
    public ReminderResponse completeReminder(Long petId, Long reminderId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        int updatedRows = reminderPersistenceMapper.completeReminder(petId, reminderId, currentUserId);
        ReminderPersistenceRecord reminder = reminderPersistenceMapper.findReminderById(reminderId);
        if (updatedRows == 0 && reminder == null) {
            throw new BusinessException(ResponseCode.REMINDER_NOT_FOUND);
        }
        return toReminderResponse(reminder);
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private ReminderResponse toReminderResponse(ReminderPersistenceRecord reminder) {
        return new ReminderResponse(
            String.valueOf(reminder.reminderId()),
            String.valueOf(reminder.petId()),
            reminder.reminderType(),
            reminder.title(),
            DateTimeConverters.toOffsetDateTime(reminder.dueAt()),
            toApiStatus(reminder.status()),
            reminder.notes(),
            DateTimeConverters.toOffsetDateTime(reminder.handledAt()),
            DateTimeConverters.toOffsetDateTime(reminder.createdAt())
        );
    }

    private String toApiStatus(String databaseStatus) {
        return "done".equals(databaseStatus) ? "completed" : databaseStatus;
    }
}
