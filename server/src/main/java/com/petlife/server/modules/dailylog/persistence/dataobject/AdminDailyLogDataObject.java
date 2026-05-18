package com.petlife.server.modules.dailylog.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台萌宠日常数据对象。
 */
public record AdminDailyLogDataObject(
    Long dailyLogId,
    Long petId,
    Long authorUserId,
    String content,
    String mediaListJson,
    String tagsJson,
    String visibility,
    Boolean syncToCommunity,
    Long communityPostId,
    LocalDateTime happenedAt,
    LocalDateTime createdAt,
    String petName,
    String petType,
    Long familyId,
    String familyName,
    Long ownerUserId,
    String ownerNickname,
    String ownerMobile,
    String authorNickname,
    String authorMobile
) {
}
