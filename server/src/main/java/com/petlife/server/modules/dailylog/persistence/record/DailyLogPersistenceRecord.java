package com.petlife.server.modules.dailylog.persistence.record;

import java.time.LocalDateTime;

/**
 * 萌宠日常持久化记录。
 *
 * @param dailyLogId 日常记录 ID
 * @param petId 宠物 ID
 * @param authorUserId 作者用户 ID
 * @param content 内容
 * @param tagsJson 标签 JSON
 * @param visibility 可见范围
 * @param happenedAt 发生时间
 * @param createdAt 创建时间
 */
public record DailyLogPersistenceRecord(
    Long dailyLogId,
    Long petId,
    Long authorUserId,
    String content,
    String tagsJson,
    String visibility,
    LocalDateTime happenedAt,
    LocalDateTime createdAt
) {
}
