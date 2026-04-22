package com.petlife.server.modules.family.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 家庭邀请响应。
 *
 * @param invitationId 邀请 ID
 * @param inviteeMobile 被邀请手机号
 * @param role 邀请角色
 * @param sharedPetIds 共享宠物 ID 列表
 * @param inviteCode 邀请码
 * @param status 邀请状态
 * @param expiredAt 过期时间
 * @param createdAt 创建时间
 */
public record FamilyInvitationResponse(
    String invitationId,
    String inviteeMobile,
    String role,
    List<String> sharedPetIds,
    String inviteCode,
    String status,
    OffsetDateTime expiredAt,
    OffsetDateTime createdAt
) {
}
