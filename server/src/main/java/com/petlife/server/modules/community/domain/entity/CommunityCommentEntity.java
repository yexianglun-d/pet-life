package com.petlife.server.modules.community.domain.entity;

import java.time.LocalDateTime;

/**
 * 社区评论实体。
 */
public final class CommunityCommentEntity {

    private final Long commentId;
    private final Long postId;
    private final String content;
    private final LocalDateTime createdAt;
    private final CommunityAuthorEntity author;

    public CommunityCommentEntity(
        Long commentId,
        Long postId,
        String content,
        LocalDateTime createdAt,
        CommunityAuthorEntity author
    ) {
        this.commentId = commentId;
        this.postId = postId;
        this.content = content;
        this.createdAt = createdAt;
        this.author = author;
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getPostId() {
        return postId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CommunityAuthorEntity getAuthor() {
        return author;
    }
}
