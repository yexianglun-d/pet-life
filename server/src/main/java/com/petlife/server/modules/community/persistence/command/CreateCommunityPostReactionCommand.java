package com.petlife.server.modules.community.persistence.command;

/**
 * 创建社区帖子点赞命令。
 */
public class CreateCommunityPostReactionCommand {

    private Long postId;
    private Long userId;
    private String reactionType;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}
