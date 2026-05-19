package com.petlife.server.modules.community.persistence.command;

/**
 * 更新社区帖子审核状态命令。
 */
public class UpdateCommunityPostReviewStatusCommand {

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
