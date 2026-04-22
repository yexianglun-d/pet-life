package com.petlife.server.modules.community.dto.response;

/**
 * 社区帖子作者响应。
 *
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 头像地址
 */
public record CommunityAuthorResponse(
    String userId,
    String nickname,
    String avatarUrl
) {
}
