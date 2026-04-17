package com.petlife.server.modules.dailylog.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 萌宠日常响应。
 *
 * @param dailyLogId 日常记录 ID
 * @param petId 宠物 ID
 * @param content 内容
 * @param tags 标签
 * @param visibility 可见范围
 * @param happenedAt 记录时间
 * @param createdAt 创建时间
 */
public record DailyLogResponse(
    String dailyLogId,
    String petId,
    String content,
    List<String> tags,
    String visibility,
    OffsetDateTime happenedAt,
    OffsetDateTime createdAt
) {
}
