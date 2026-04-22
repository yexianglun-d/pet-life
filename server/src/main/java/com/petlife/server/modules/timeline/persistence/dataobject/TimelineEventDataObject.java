package com.petlife.server.modules.timeline.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 时间轴事件数据对象。
 *
 * @param eventId 事件 ID
 * @param petId 宠物 ID
 * @param eventType 事件类型
 * @param sourceType 来源类型
 * @param sourceId 来源记录 ID
 * @param eventTime 事件时间
 * @param title 标题
 * @param summary 摘要
 * @param coverUrl 封面图
 * @param visibility 可见范围
 * @param createdAt 创建时间
 */
public record TimelineEventDataObject(
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
}
