package com.petlife.server.modules.reminder.persistence.command;

/**
 * 提醒模板启停命令。
 */
public class UpdateReminderTemplateStatusCommand {

    private Long templateId;
    private boolean enabled;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
