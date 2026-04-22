package com.petlife.server.modules.health.persistence.command;

/**
 * 删除健康记录命令。
 */
public class DeleteHealthRecordCommand {

    private Long healthRecordId;
    private Long petId;

    public Long getHealthRecordId() {
        return healthRecordId;
    }

    public void setHealthRecordId(Long healthRecordId) {
        this.healthRecordId = healthRecordId;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }
}
