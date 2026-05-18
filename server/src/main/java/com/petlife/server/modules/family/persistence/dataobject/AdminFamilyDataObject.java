package com.petlife.server.modules.family.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台家庭查询数据对象。
 */
public record AdminFamilyDataObject(
    Long familyId,
    String familyName,
    Long ownerUserId,
    String ownerNickname,
    String ownerMobile,
    Integer status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer memberCount,
    Integer petCount
) {
}
