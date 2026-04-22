package com.petlife.server.modules.family.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 家庭成员数据对象。
 *
 * @param memberId 成员关系 ID
 * @param familyId 家庭 ID
 * @param userId 用户 ID
 * @param nickname 昵称
 * @param mobile 手机号
 * @param role 角色
 * @param inviteStatus 邀请状态
 * @param joinedAt 加入时间
 */
public record FamilyMemberDataObject(
    Long memberId,
    Long familyId,
    Long userId,
    String nickname,
    String mobile,
    String role,
    String inviteStatus,
    LocalDateTime joinedAt
) {
}
