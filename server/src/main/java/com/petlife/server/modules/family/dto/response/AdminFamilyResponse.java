package com.petlife.server.modules.family.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 后台家庭响应。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param owner 家庭拥有者
 * @param status 家庭状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @param memberCount 成员数
 * @param petCount 宠物数
 * @param members 成员关系
 * @param pets 家庭宠物
 */
public record AdminFamilyResponse(
    String familyId,
    String familyName,
    AdminUserContextResponse owner,
    Integer status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Integer memberCount,
    Integer petCount,
    List<FamilyMemberResponse> members,
    List<AdminFamilyPetResponse> pets
) {
}
