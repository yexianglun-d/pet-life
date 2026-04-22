package com.petlife.server.modules.community.persistence.command;

/**
 * 删除社区帖子命令。
 */
public class DeleteCommunityPostCommand {

    private Long postId;
    private Long sourceDailyLogId;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getSourceDailyLogId() {
        return sourceDailyLogId;
    }

    public void setSourceDailyLogId(Long sourceDailyLogId) {
        this.sourceDailyLogId = sourceDailyLogId;
    }
}
