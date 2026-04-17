package com.petlife.server.bootstrap.devsupport.model;

/**
 * 开发期家庭摘要模型。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param memberCount 家庭成员数
 * @param role 当前用户在家庭中的角色
 */
public record DevFamilySummary(
    Long familyId,
    String familyName,
    Integer memberCount,
    String role
) {
}
