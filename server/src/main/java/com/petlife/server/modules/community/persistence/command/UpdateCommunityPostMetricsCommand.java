package com.petlife.server.modules.community.persistence.command;

/**
 * 更新社区帖子聚合计数命令。
 */
public class UpdateCommunityPostMetricsCommand {

    private Long postId;
    private Integer likeDelta;
    private Integer commentDelta;
    private Integer favoriteDelta;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Integer getLikeDelta() {
        return likeDelta;
    }

    public void setLikeDelta(Integer likeDelta) {
        this.likeDelta = likeDelta;
    }

    public Integer getCommentDelta() {
        return commentDelta;
    }

    public void setCommentDelta(Integer commentDelta) {
        this.commentDelta = commentDelta;
    }

    public Integer getFavoriteDelta() {
        return favoriteDelta;
    }

    public void setFavoriteDelta(Integer favoriteDelta) {
        this.favoriteDelta = favoriteDelta;
    }
}
