package com.petlife.server.modules.community.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 社区话题数据对象。
 *
 * @param topicId 话题 ID
 * @param topicName 话题名称
 * @param topicDesc 话题说明
 * @param cityCode 城市编码
 * @param status 状态：1-启用 0-停用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record CommunityTopicDataObject(
    Long topicId,
    String topicName,
    String topicDesc,
    String cityCode,
    Integer status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
