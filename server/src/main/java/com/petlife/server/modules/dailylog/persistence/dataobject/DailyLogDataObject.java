package com.petlife.server.modules.dailylog.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 萌宠日常数据对象。
 *
 * @param dailyLogId 日常记录 ID
 * @param petId 宠物 ID
 * @param authorUserId 作者用户 ID
 * @param content 内容
 * @param tagsJson 标签 JSON
 * @param visibility 可见范围
 * @param syncToCommunity 是否同步社区
 * @param communityPostId 社区帖子 ID
 * @param happenedAt 发生时间
 * @param createdAt 创建时间
 */
public record DailyLogDataObject(
    Long dailyLogId,
    Long petId,
    Long authorUserId,
    String content,
    String tagsJson,
    String visibility,
    Boolean syncToCommunity,
    Long communityPostId,
    LocalDateTime happenedAt,
    LocalDateTime createdAt
) {
}
