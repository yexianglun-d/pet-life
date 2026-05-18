package com.petlife.server.modules.pet.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;

/**
 * 后台宠物响应。
 *
 * @param pet 宠物详情
 * @param owner 主人上下文
 * @param family 家庭归属上下文
 */
public record AdminPetResponse(
    PetDetailResponse pet,
    AdminUserContextResponse owner,
    AdminPetFamilyResponse family
) {
}
