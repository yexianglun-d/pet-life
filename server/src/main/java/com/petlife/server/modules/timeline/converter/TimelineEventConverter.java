package com.petlife.server.modules.timeline.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.timeline.domain.entity.TimelineEventEntity;
import com.petlife.server.modules.timeline.dto.response.TimelineEventResponse;
import com.petlife.server.modules.timeline.persistence.dataobject.TimelineEventDataObject;
import org.springframework.stereotype.Component;

/**
 * 时间轴事件转换器。
 */
@Component
public class TimelineEventConverter {

    public TimelineEventEntity toEntity(TimelineEventDataObject timelineEventDataObject) {
        if (timelineEventDataObject == null) {
            return null;
        }

        return new TimelineEventEntity(
            timelineEventDataObject.eventId(),
            timelineEventDataObject.petId(),
            timelineEventDataObject.eventType(),
            timelineEventDataObject.sourceType(),
            timelineEventDataObject.sourceId(),
            timelineEventDataObject.eventTime(),
            timelineEventDataObject.title(),
            timelineEventDataObject.summary(),
            timelineEventDataObject.coverUrl(),
            timelineEventDataObject.visibility(),
            timelineEventDataObject.createdAt()
        );
    }

    public TimelineEventResponse toResponse(TimelineEventEntity timelineEvent) {
        return new TimelineEventResponse(
            String.valueOf(timelineEvent.getEventId()),
            String.valueOf(timelineEvent.getPetId()),
            timelineEvent.getEventType(),
            timelineEvent.getSourceType(),
            String.valueOf(timelineEvent.getSourceId()),
            DateTimeConverters.toOffsetDateTime(timelineEvent.getEventTime()),
            timelineEvent.getTitle(),
            timelineEvent.getSummary(),
            timelineEvent.getCoverUrl(),
            timelineEvent.getVisibility(),
            DateTimeConverters.toOffsetDateTime(timelineEvent.getCreatedAt())
        );
    }
}
