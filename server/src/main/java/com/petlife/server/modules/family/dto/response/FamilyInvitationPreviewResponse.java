package com.petlife.server.modules.family.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 家庭邀请预览响应。
 *
 * @param invitationId 邀请 ID
 * @param familyId 家庭 ID
 * @param familyName 家庭名称
 * @param inviterNickname 邀请人昵称
 * @param inviteeMobile 被邀请手机号
 * @param role 邀请角色
 * @param sharedPets 共享宠物列表
 * @param inviteCode 邀请码
 * @param status 邀请状态
 * @param expiredAt 过期时间
 * @param createdAt 创建时间
 */
public record FamilyInvitationPreviewResponse(
    String invitationId,
    String familyId,
    String familyName,
    String inviterNickname,
    String inviteeMobile,
    String role,
    List<FamilySharedPetResponse> sharedPets,
    String inviteCode,
    String status,
    OffsetDateTime expiredAt,
    OffsetDateTime createdAt
) {
}
