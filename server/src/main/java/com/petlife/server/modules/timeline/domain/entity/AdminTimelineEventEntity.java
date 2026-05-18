package com.petlife.server.modules.timeline.domain.entity;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;

/**
 * 后台时间轴事件聚合实体。
 */
public final class AdminTimelineEventEntity {

    private final TimelineEventEntity timelineEvent;
    private final String sourceStatus;
    private final AdminPetContextEntity petContext;

    public AdminTimelineEventEntity(
        TimelineEventEntity timelineEvent,
        String sourceStatus,
        AdminPetContextEntity petContext
    ) {
        this.timelineEvent = timelineEvent;
        this.sourceStatus = sourceStatus;
        this.petContext = petContext;
    }

    public TimelineEventEntity getTimelineEvent() {
        return timelineEvent;
    }

    public String getSourceStatus() {
        return sourceStatus;
    }

    public AdminPetContextEntity getPetContext() {
        return petContext;
    }
}
