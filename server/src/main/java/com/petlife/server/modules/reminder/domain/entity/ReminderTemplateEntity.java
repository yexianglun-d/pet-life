package com.petlife.server.modules.reminder.domain.entity;

import java.time.LocalDateTime;

/**
 * 提醒模板领域实体。
 */
public final class ReminderTemplateEntity {

    private final Long templateId;
    private final String templateName;
    private final String reminderType;
    private final String defaultReminderMode;
    private final Integer defaultAdvanceValue;
    private final String defaultAdvanceUnit;
    private final Integer defaultCycleValue;
    private final String defaultCycleUnit;
    private final String applicablePetType;
    private final boolean enabled;
    private final Integer sortOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ReminderTemplateEntity(
        Long templateId,
        String templateName,
        String reminderType,
        String defaultReminderMode,
        Integer defaultAdvanceValue,
        String defaultAdvanceUnit,
        Integer defaultCycleValue,
        String defaultCycleUnit,
        String applicablePetType,
        boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.templateId = templateId;
        this.templateName = templateName;
        this.reminderType = reminderType;
        this.defaultReminderMode = defaultReminderMode;
        this.defaultAdvanceValue = defaultAdvanceValue;
        this.defaultAdvanceUnit = defaultAdvanceUnit;
        this.defaultCycleValue = defaultCycleValue;
        this.defaultCycleUnit = defaultCycleUnit;
        this.applicablePetType = applicablePetType;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getReminderType() {
        return reminderType;
    }

    public String getDefaultReminderMode() {
        return defaultReminderMode;
    }

    public Integer getDefaultAdvanceValue() {
        return defaultAdvanceValue;
    }

    public String getDefaultAdvanceUnit() {
        return defaultAdvanceUnit;
    }

    public Integer getDefaultCycleValue() {
        return defaultCycleValue;
    }

    public String getDefaultCycleUnit() {
        return defaultCycleUnit;
    }

    public String getApplicablePetType() {
        return applicablePetType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
