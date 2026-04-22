package com.petlife.server.modules.family.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 家庭邀请数据对象。
 *
 * @param invitationId 邀请 ID
 * @param familyId 家庭 ID
 * @param inviterUserId 邀请人用户 ID
 * @param inviteeMobile 被邀请手机号
 * @param inviteeUserId 被邀请用户 ID
 * @param role 邀请角色
 * @param sharedPetIdsJson 共享宠物 JSON
 * @param inviteCode 邀请码
 * @param status 邀请状态
 * @param expiredAt 过期时间
 * @param acceptedAt 接受时间
 * @param createdAt 创建时间
 */
public record FamilyInvitationDataObject(
    Long invitationId,
    Long familyId,
    Long inviterUserId,
    String inviteeMobile,
    Long inviteeUserId,
    String role,
    String sharedPetIdsJson,
    String inviteCode,
    String status,
    LocalDateTime expiredAt,
    LocalDateTime acceptedAt,
    LocalDateTime createdAt
) {
}
