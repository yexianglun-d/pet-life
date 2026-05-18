package com.petlife.server.modules.family.dto.response;

/**
 * 后台家庭宠物响应。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 * @param status 宠物状态
 * @param ownerUserId 主人用户 ID
 * @param ownerNickname 主人昵称
 * @param ownerMobile 主人手机号
 */
public record AdminFamilyPetResponse(
    String petId,
    String petName,
    String petType,
    String breed,
    String status,
    String ownerUserId,
    String ownerNickname,
    String ownerMobile
) {
}
