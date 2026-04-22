package com.petlife.server.modules.timeline.persistence.command;

/**
 * 删除时间轴事件命令。
 */
public class DeleteTimelineEventCommand {

    private Long petId;
    private String sourceType;
    private Long sourceId;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}
