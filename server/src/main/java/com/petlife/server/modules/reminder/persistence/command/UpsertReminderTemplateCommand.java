package com.petlife.server.modules.reminder.persistence.command;

/**
 * 提醒模板创建或更新命令。
 */
public class UpsertReminderTemplateCommand {

    private Long templateId;
    private String templateName;
    private String reminderType;
    private String defaultReminderMode;
    private Integer defaultAdvanceValue;
    private String defaultAdvanceUnit;
    private Integer defaultCycleValue;
    private String defaultCycleUnit;
    private String applicablePetType;
    private boolean enabled;
    private Integer sortOrder;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getDefaultReminderMode() {
        return defaultReminderMode;
    }

    public void setDefaultReminderMode(String defaultReminderMode) {
        this.defaultReminderMode = defaultReminderMode;
    }

    public Integer getDefaultAdvanceValue() {
        return defaultAdvanceValue;
    }

    public void setDefaultAdvanceValue(Integer defaultAdvanceValue) {
        this.defaultAdvanceValue = defaultAdvanceValue;
    }

    public String getDefaultAdvanceUnit() {
        return defaultAdvanceUnit;
    }

    public void setDefaultAdvanceUnit(String defaultAdvanceUnit) {
        this.defaultAdvanceUnit = defaultAdvanceUnit;
    }

    public Integer getDefaultCycleValue() {
        return defaultCycleValue;
    }

    public void setDefaultCycleValue(Integer defaultCycleValue) {
        this.defaultCycleValue = defaultCycleValue;
    }

    public String getDefaultCycleUnit() {
        return defaultCycleUnit;
    }

    public void setDefaultCycleUnit(String defaultCycleUnit) {
        this.defaultCycleUnit = defaultCycleUnit;
    }

    public String getApplicablePetType() {
        return applicablePetType;
    }

    public void setApplicablePetType(String applicablePetType) {
        this.applicablePetType = applicablePetType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
