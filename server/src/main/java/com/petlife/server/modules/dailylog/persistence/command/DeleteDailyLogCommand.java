package com.petlife.server.modules.dailylog.persistence.command;

/**
 * 删除萌宠日常命令。
 */
public class DeleteDailyLogCommand {

    private Long petId;
    private Long dailyLogId;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Long getDailyLogId() {
        return dailyLogId;
    }

    public void setDailyLogId(Long dailyLogId) {
        this.dailyLogId = dailyLogId;
    }
}
