package com.petlife.server.modules.community.dto.response;

import java.util.List;

/**
 * 社区话题页响应。
 *
 * @param topic 话题信息
 * @param posts 话题下帖子列表
 */
public record CommunityTopicDetailResponse(
    CommunityTopicResponse topic,
    List<CommunityPostResponse> posts
) {
}
