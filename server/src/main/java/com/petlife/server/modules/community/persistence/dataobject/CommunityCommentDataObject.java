package com.petlife.server.modules.community.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 社区评论数据对象。
 *
 * @param commentId 评论 ID
 * @param postId 帖子 ID
 * @param content 评论内容
 * @param createdAt 创建时间
 * @param authorUserId 作者用户 ID
 * @param authorNickname 作者昵称
 * @param authorAvatarUrl 作者头像
 */
public record CommunityCommentDataObject(
    Long commentId,
    Long postId,
    String content,
    LocalDateTime createdAt,
    Long authorUserId,
    String authorNickname,
    String authorAvatarUrl
) {
}
