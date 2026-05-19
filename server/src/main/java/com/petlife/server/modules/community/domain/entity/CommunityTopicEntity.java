package com.petlife.server.modules.community.domain.entity;

import java.time.LocalDateTime;

/**
 * 社区话题实体。
 */
public final class CommunityTopicEntity {

    private final Long topicId;
    private final String topicName;
    private final String topicDesc;
    private final String cityCode;
    private final Integer status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CommunityTopicEntity(
        Long topicId,
        String topicName,
        String topicDesc,
        String cityCode,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.topicDesc = topicDesc;
        this.cityCode = cityCode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getTopicDesc() {
        return topicDesc;
    }

    public String getCityCode() {
        return cityCode;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
