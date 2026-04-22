package com.petlife.server.modules.family.persistence.dataobject;

/**
 * 家庭档案数据对象。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param ownerUserId 家庭拥有者用户 ID
 * @param memberCount 成员数量
 * @param currentUserRole 当前用户角色
 */
public record FamilyProfileDataObject(
    Long familyId,
    String familyName,
    Long ownerUserId,
    Integer memberCount,
    String currentUserRole
) {
}
