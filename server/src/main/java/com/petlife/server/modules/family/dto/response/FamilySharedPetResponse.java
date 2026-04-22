package com.petlife.server.modules.family.dto.response;

/**
 * 家庭共享宠物响应。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 */
public record FamilySharedPetResponse(
    String petId,
    String petName,
    String petType,
    String breed
) {
}
