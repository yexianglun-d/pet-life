package com.petlife.server.modules.timeline.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台时间轴事件数据对象。
 */
public record AdminTimelineEventDataObject(
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
    LocalDateTime createdAt,
    String sourceStatus,
    String petName,
    String petType,
    Long familyId,
    String familyName,
    Long ownerUserId,
    String ownerNickname,
    String ownerMobile
) {
}
