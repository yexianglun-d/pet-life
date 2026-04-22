package com.petlife.server.modules.dailylog.persistence.command;

/**
 * 更新萌宠日常与社区帖子绑定关系命令。
 */
public class UpdateDailyLogCommunityBindingCommand {

    private Long dailyLogId;
    private Long communityPostId;
    private Boolean syncToCommunity;

    public Long getDailyLogId() {
        return dailyLogId;
    }

    public void setDailyLogId(Long dailyLogId) {
        this.dailyLogId = dailyLogId;
    }

    public Long getCommunityPostId() {
        return communityPostId;
    }

    public void setCommunityPostId(Long communityPostId) {
        this.communityPostId = communityPostId;
    }

    public Boolean getSyncToCommunity() {
        return syncToCommunity;
    }

    public void setSyncToCommunity(Boolean syncToCommunity) {
        this.syncToCommunity = syncToCommunity;
    }
}
