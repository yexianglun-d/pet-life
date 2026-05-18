package com.petlife.server.modules.reminder.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 提醒模板持久化读模型。
 */
public record ReminderTemplateDataObject(
    Long templateId,
    String templateName,
    String reminderType,
    String defaultReminderMode,
    Integer defaultAdvanceValue,
    String defaultAdvanceUnit,
    Integer defaultCycleValue,
    String defaultCycleUnit,
    String applicablePetType,
    Boolean enabled,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
