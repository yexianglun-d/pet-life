package com.petlife.server.modules.moderation.persistence.command;

/**
 * 更新被举报帖子审核状态命令。
 */
public class UpdateModerationTargetPostReviewStatusCommand {

    private Long postId;
    private String reviewStatus;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
}
