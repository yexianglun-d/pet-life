package com.petlife.server.modules.auth.dto.response;

/**
 * 登录态宠物摘要。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 */
public record AuthPetSummaryResponse(
    String petId,
    String petName,
    String petType,
    String breed
) {
}
