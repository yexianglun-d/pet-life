package com.petlife.server.modules.user.persistence.dataobject;

/**
 * 用户家庭摘要数据对象。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param memberCount 成员数量
 * @param role 当前用户在家庭中的角色
 */
public record FamilySummaryDataObject(
    Long familyId,
    String familyName,
    Integer memberCount,
    String role
) {
}
