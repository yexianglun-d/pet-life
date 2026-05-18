package com.petlife.server.modules.user.dto.response;

/**
 * 后台用户家庭摘要响应。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param role 当前用户在家庭中的角色
 * @param memberCount 家庭已加入成员数
 */
public record AdminUserFamilyResponse(
    String familyId,
    String familyName,
    String role,
    Integer memberCount
) {
}
