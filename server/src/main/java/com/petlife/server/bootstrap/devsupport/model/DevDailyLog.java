package com.petlife.server.bootstrap.devsupport.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 开发期萌宠日常模型。
 *
 * @param dailyLogId 日常记录 ID
 * @param petId 宠物 ID
 * @param content 内容
 * @param tags 标签
 * @param visibility 可见范围
 * @param happenedAt 记录发生时间
 * @param createdAt 创建时间
 */
public record DevDailyLog(
    Long dailyLogId,
    Long petId,
    String content,
    List<String> tags,
    String visibility,
    OffsetDateTime happenedAt,
    OffsetDateTime createdAt
) {
}
