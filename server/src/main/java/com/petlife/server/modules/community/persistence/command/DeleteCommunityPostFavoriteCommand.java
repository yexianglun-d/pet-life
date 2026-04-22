package com.petlife.server.modules.community.persistence.command;

/**
 * 删除社区帖子收藏命令。
 */
public class DeleteCommunityPostFavoriteCommand {

    private Long postId;
    private Long userId;

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
}
