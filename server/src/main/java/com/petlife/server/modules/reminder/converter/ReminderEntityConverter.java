package com.petlife.server.modules.reminder.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import com.petlife.server.modules.reminder.persistence.dataobject.ReminderDataObject;
import org.springframework.stereotype.Component;

/**
 * 提醒实体转换器。
 */
@Component
public class ReminderEntityConverter {

    public ReminderEntity toEntity(ReminderDataObject reminderDataObject) {
        if (reminderDataObject == null) {
            return null;
        }

        return new ReminderEntity(
            reminderDataObject.reminderId(),
            reminderDataObject.petId(),
            reminderDataObject.reminderType(),
            reminderDataObject.title(),
            reminderDataObject.reminderMode(),
            reminderDataObject.cycleValue(),
            reminderDataObject.cycleUnit(),
            reminderDataObject.dueAt(),
            normalizeStatus(reminderDataObject.status()),
            reminderDataObject.notes(),
            reminderDataObject.handledAt(),
            reminderDataObject.createdAt()
        );
    }

    public ReminderResponse toResponse(ReminderEntity reminder) {
        return new ReminderResponse(
            String.valueOf(reminder.getReminderId()),
            String.valueOf(reminder.getPetId()),
            reminder.getReminderType(),
            reminder.getTitle(),
            reminder.getReminderMode(),
            reminder.getCycleValue(),
            reminder.getCycleUnit(),
            DateTimeConverters.toOffsetDateTime(reminder.getDueAt()),
            reminder.getStatus(),
            reminder.getNotes(),
            DateTimeConverters.toOffsetDateTime(reminder.getHandledAt()),
            DateTimeConverters.toOffsetDateTime(reminder.getCreatedAt())
        );
    }

    private String normalizeStatus(String databaseStatus) {
        return "done".equals(databaseStatus) ? "completed" : databaseStatus;
    }
}
