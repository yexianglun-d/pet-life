package com.petlife.server.modules.timeline.domain.entity;

import java.time.LocalDateTime;

/**
 * 时间轴事件实体。
 *
 * <p>时间轴是跨业务聚合的只读视图，不回写事实表。
 * 因此该实体只承载展示所需字段和来源标识，不承担任何源记录状态修改职责。</p>
 */
public final class TimelineEventEntity {

    private final Long eventId;
    private final Long petId;
    private final String eventType;
    private final String sourceType;
    private final Long sourceId;
    private final LocalDateTime eventTime;
    private final String title;
    private final String summary;
    private final String coverUrl;
    private final String visibility;
    private final LocalDateTime createdAt;

    public TimelineEventEntity(
        Long eventId,
        Long petId,
        String eventType,
        String sourceType,
        Long sourceId,
        LocalDateTime eventTime,
        String title,
        String summary,
        String coverUrl,
        String visibility,
        LocalDateTime createdAt
    ) {
        this.eventId = eventId;
        this.petId = petId;
        this.eventType = eventType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.eventTime = eventTime;
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        this.visibility = visibility;
        this.createdAt = createdAt;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getPetId() {
        return petId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
