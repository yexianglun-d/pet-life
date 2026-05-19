package com.petlife.server.modules.community.dto.response;

import java.time.OffsetDateTime;

/**
 * 社区话题响应。
 *
 * @param topicId 话题 ID
 * @param topicName 话题名称
 * @param topicDesc 话题说明
 * @param cityCode 城市编码
 * @param status 状态：1-启用 0-停用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record CommunityTopicResponse(
    String topicId,
    String topicName,
    String topicDesc,
    String cityCode,
    Integer status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
