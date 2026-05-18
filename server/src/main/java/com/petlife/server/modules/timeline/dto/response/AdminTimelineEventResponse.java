package com.petlife.server.modules.timeline.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;

/**
 * 后台时间轴事件响应。
 *
 * @param timelineEvent 时间轴事件
 * @param pet 宠物归属上下文
 * @param sourceStatus 源记录状态：active/deleted/missing/unsupported
 */
public record AdminTimelineEventResponse(
    TimelineEventResponse timelineEvent,
    AdminPetContextResponse pet,
    String sourceStatus
) {
}
