package com.petlife.server.modules.pet.dto.response;

/**
 * 后台宠物家庭归属响应。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param status 家庭状态
 * @param memberCount 家庭成员数
 */
public record AdminPetFamilyResponse(
    String familyId,
    String familyName,
    Integer status,
    Integer memberCount
) {
}
