package com.petlife.server.modules.timeline.converter;

import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.timeline.domain.entity.AdminTimelineEventEntity;
import com.petlife.server.modules.timeline.domain.entity.TimelineEventEntity;
import com.petlife.server.modules.timeline.dto.response.AdminTimelineEventResponse;
import com.petlife.server.modules.timeline.persistence.dataobject.AdminTimelineEventDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台时间轴事件转换器。
 */
@Component
public class AdminTimelineEventConverter {

    private final TimelineEventConverter timelineEventConverter;
    private final AdminContextConverter adminContextConverter;

    public AdminTimelineEventConverter(
        TimelineEventConverter timelineEventConverter,
        AdminContextConverter adminContextConverter
    ) {
        this.timelineEventConverter = timelineEventConverter;
        this.adminContextConverter = adminContextConverter;
    }

    public AdminTimelineEventEntity toEntity(AdminTimelineEventDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        TimelineEventEntity timelineEvent = new TimelineEventEntity(
            dataObject.eventId(),
            dataObject.petId(),
            dataObject.eventType(),
            dataObject.sourceType(),
            dataObject.sourceId(),
            dataObject.eventTime(),
            dataObject.title(),
            dataObject.summary(),
            dataObject.coverUrl(),
            dataObject.visibility(),
            dataObject.createdAt()
        );
        return new AdminTimelineEventEntity(
            timelineEvent,
            dataObject.sourceStatus(),
            new AdminPetContextEntity(
                dataObject.petId(),
                dataObject.petName(),
                dataObject.petType(),
                dataObject.familyId(),
                dataObject.familyName(),
                dataObject.ownerUserId(),
                dataObject.ownerNickname(),
                dataObject.ownerMobile()
            )
        );
    }

    public AdminTimelineEventResponse toResponse(AdminTimelineEventEntity entity) {
        return new AdminTimelineEventResponse(
            timelineEventConverter.toResponse(entity.getTimelineEvent()),
            adminContextConverter.toPetResponse(entity.getPetContext()),
            entity.getSourceStatus()
        );
    }
}
