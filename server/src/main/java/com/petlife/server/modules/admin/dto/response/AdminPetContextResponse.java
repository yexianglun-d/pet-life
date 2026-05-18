package com.petlife.server.modules.admin.dto.response;

/**
 * 后台宠物上下文响应。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param ownerUserId 主人用户 ID
 * @param ownerNickname 主人昵称
 * @param ownerMobile 主人手机号
 */
public record AdminPetContextResponse(
    String petId,
    String petName,
    String petType,
    String familyId,
    String familyName,
    String ownerUserId,
    String ownerNickname,
    String ownerMobile
) {
}
