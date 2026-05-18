package com.petlife.server.modules.reminder.converter;

import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.reminder.domain.entity.AdminReminderEntity;
import com.petlife.server.modules.reminder.domain.entity.AdminReminderSourceEntity;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.dto.response.AdminReminderResponse;
import com.petlife.server.modules.reminder.dto.response.AdminReminderSourceResponse;
import com.petlife.server.modules.reminder.persistence.dataobject.AdminReminderDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台提醒转换器。
 */
@Component
public class AdminReminderConverter {

    private final ReminderEntityConverter reminderEntityConverter;
    private final AdminContextConverter adminContextConverter;

    public AdminReminderConverter(
        ReminderEntityConverter reminderEntityConverter,
        AdminContextConverter adminContextConverter
    ) {
        this.reminderEntityConverter = reminderEntityConverter;
        this.adminContextConverter = adminContextConverter;
    }

    public AdminReminderEntity toEntity(AdminReminderDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        ReminderEntity reminder = new ReminderEntity(
            dataObject.reminderId(),
            dataObject.petId(),
            dataObject.reminderType(),
            dataObject.title(),
            dataObject.reminderMode(),
            dataObject.cycleValue(),
            dataObject.cycleUnit(),
            dataObject.dueAt(),
            normalizeReminderStatus(dataObject.status()),
            dataObject.notes(),
            dataObject.handledAt(),
            dataObject.createdAt()
        );
        AdminUserContextEntity handlerContext = dataObject.handlerUserId() == null ? null : new AdminUserContextEntity(
            dataObject.handlerUserId(),
            dataObject.handlerNickname(),
            dataObject.handlerMobile()
        );
        AdminReminderSourceEntity sourceContext = dataObject.sourceRecordId() == null ? null : new AdminReminderSourceEntity(
            dataObject.sourceRecordId(),
            dataObject.sourceRecordType(),
            dataObject.sourceRecordTitle(),
            dataObject.sourceRecordStatus()
        );
        return new AdminReminderEntity(
            reminder,
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
            handlerContext,
            sourceContext
        );
    }

    public AdminReminderResponse toResponse(AdminReminderEntity entity) {
        return new AdminReminderResponse(
            reminderEntityConverter.toResponse(entity.getReminder()),
            adminContextConverter.toPetResponse(entity.getPetContext()),
            adminContextConverter.toUserResponse(entity.getHandlerContext()),
            toSourceResponse(entity.getSourceContext())
        );
    }

    private AdminReminderSourceResponse toSourceResponse(AdminReminderSourceEntity sourceEntity) {
        if (sourceEntity == null) {
            return null;
        }
        return new AdminReminderSourceResponse(
            String.valueOf(sourceEntity.getSourceRecordId()),
            sourceEntity.getRecordType(),
            sourceEntity.getTitle(),
            sourceEntity.getStatus()
        );
    }

    private String normalizeReminderStatus(String databaseStatus) {
        return "done".equals(databaseStatus) ? "completed" : databaseStatus;
    }
}
