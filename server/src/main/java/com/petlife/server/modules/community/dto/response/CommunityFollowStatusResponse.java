package com.petlife.server.modules.community.dto.response;

/**
 * 社区关注状态响应。
 *
 * @param followedUserId 被关注用户 ID
 * @param following 当前用户是否已关注
 */
public record CommunityFollowStatusResponse(
    String followedUserId,
    Boolean following
) {
}
