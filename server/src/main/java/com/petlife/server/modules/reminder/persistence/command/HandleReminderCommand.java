package com.petlife.server.modules.reminder.persistence.command;

/**
 * 处理提醒命令。
 */
public class HandleReminderCommand {

    private Long petId;
    private Long reminderId;
    private Long handledByUserId;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public Long getHandledByUserId() {
        return handledByUserId;
    }

    public void setHandledByUserId(Long handledByUserId) {
        this.handledByUserId = handledByUserId;
    }
}
