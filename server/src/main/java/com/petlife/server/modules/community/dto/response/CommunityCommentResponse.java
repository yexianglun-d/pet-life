package com.petlife.server.modules.community.dto.response;

import java.time.OffsetDateTime;

/**
 * 社区评论响应。
 *
 * @param commentId 评论 ID
 * @param postId 帖子 ID
 * @param content 评论内容
 * @param createdAt 创建时间
 * @param author 作者摘要
 */
public record CommunityCommentResponse(
    String commentId,
    String postId,
    String content,
    OffsetDateTime createdAt,
    CommunityAuthorResponse author
) {
}
