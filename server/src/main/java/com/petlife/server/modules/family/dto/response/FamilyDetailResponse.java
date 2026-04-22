package com.petlife.server.modules.family.dto.response;

import java.util.List;

/**
 * 家庭详情响应。
 *
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param memberCount 成员数量
 * @param currentUserRole 当前用户角色
 * @param members 家庭成员列表
 * @param sharedPets 共享宠物列表
 * @param pendingInvitations 待处理邀请列表
 */
public record FamilyDetailResponse(
    String familyId,
    String familyName,
    Integer memberCount,
    String currentUserRole,
    List<FamilyMemberResponse> members,
    List<FamilySharedPetResponse> sharedPets,
    List<FamilyInvitationResponse> pendingInvitations
) {
}
