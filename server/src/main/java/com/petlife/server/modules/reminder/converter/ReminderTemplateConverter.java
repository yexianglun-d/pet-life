package com.petlife.server.modules.reminder.converter;

import com.petlife.server.modules.reminder.domain.entity.ReminderTemplateEntity;
import com.petlife.server.modules.reminder.dto.response.ReminderTemplateResponse;
import com.petlife.server.modules.reminder.persistence.dataobject.ReminderTemplateDataObject;
import org.springframework.stereotype.Component;

/**
 * 提醒模板转换器。
 */
@Component
public class ReminderTemplateConverter {

    public ReminderTemplateEntity toEntity(ReminderTemplateDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ReminderTemplateEntity(
            dataObject.templateId(),
            dataObject.templateName(),
            dataObject.reminderType(),
            dataObject.defaultReminderMode(),
            dataObject.defaultAdvanceValue(),
            dataObject.defaultAdvanceUnit(),
            dataObject.defaultCycleValue(),
            dataObject.defaultCycleUnit(),
            dataObject.applicablePetType(),
            Boolean.TRUE.equals(dataObject.enabled()),
            dataObject.sortOrder(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ReminderTemplateResponse toResponse(ReminderTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ReminderTemplateResponse(
            String.valueOf(entity.getTemplateId()),
            entity.getTemplateName(),
            entity.getReminderType(),
            entity.getDefaultReminderMode(),
            entity.getDefaultAdvanceValue(),
            entity.getDefaultAdvanceUnit(),
            entity.getDefaultCycleValue(),
            entity.getDefaultCycleUnit(),
            entity.getApplicablePetType(),
            entity.isEnabled(),
            entity.getSortOrder(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
