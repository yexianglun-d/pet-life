package com.petlife.server.modules.community.dto.response;

/**
 * 社区帖子关联宠物响应。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 */
public record CommunityPetResponse(
    String petId,
    String petName,
    String petType,
    String breed
) {
}
