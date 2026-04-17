package com.petlife.server.modules.user.persistence.record;

/**
 * 用户家庭摘要持久化记录。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param memberCount 成员数量
 * @param role 当前用户在家庭中的角色
 */
public record FamilySummaryPersistenceRecord(
    Long familyId,
    String familyName,
    Integer memberCount,
    String role
) {
}
