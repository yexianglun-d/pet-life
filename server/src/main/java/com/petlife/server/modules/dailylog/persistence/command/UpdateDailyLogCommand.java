package com.petlife.server.modules.dailylog.persistence.command;

import java.time.LocalDateTime;

/**
 * 更新萌宠日常命令。
 */
public class UpdateDailyLogCommand {

    private Long petId;
    private Long dailyLogId;
    private String content;
    private String mediaListJson;
    private String tagsJson;
    private String visibility;
    private Boolean syncToCommunity;
    private LocalDateTime happenedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaListJson() {
        return mediaListJson;
    }

    public void setMediaListJson(String mediaListJson) {
        this.mediaListJson = mediaListJson;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Boolean getSyncToCommunity() {
        return syncToCommunity;
    }

    public void setSyncToCommunity(Boolean syncToCommunity) {
        this.syncToCommunity = syncToCommunity;
    }

    public LocalDateTime getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(LocalDateTime happenedAt) {
        this.happenedAt = happenedAt;
    }
}
