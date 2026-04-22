package com.petlife.server.modules.timeline.dto.response;

import java.time.OffsetDateTime;

/**
 * 时间轴事件响应。
 *
 * @param eventId 事件 ID
 * @param petId 宠物 ID
 * @param eventType 事件类型
 * @param sourceType 来源类型
 * @param sourceId 来源记录 ID
 * @param eventTime 事件时间
 * @param title 事件标题
 * @param summary 事件摘要
 * @param coverUrl 封面图
 * @param visibility 可见范围
 * @param createdAt 创建时间
 */
public record TimelineEventResponse(
    String eventId,
    String petId,
    String eventType,
    String sourceType,
    String sourceId,
    OffsetDateTime eventTime,
    String title,
    String summary,
    String coverUrl,
    String visibility,
    OffsetDateTime createdAt
) {
}
