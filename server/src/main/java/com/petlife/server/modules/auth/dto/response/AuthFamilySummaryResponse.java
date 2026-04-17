package com.petlife.server.modules.auth.dto.response;

/**
 * 家庭摘要响应。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param memberCount 家庭成员数量
 * @param role 当前用户角色
 */
public record AuthFamilySummaryResponse(
    String familyId,
    String familyName,
    Integer memberCount,
    String role
) {
}
