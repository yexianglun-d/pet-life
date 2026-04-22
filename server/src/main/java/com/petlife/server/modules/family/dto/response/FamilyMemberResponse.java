package com.petlife.server.modules.family.dto.response;

import java.time.OffsetDateTime;

/**
 * 家庭成员响应。
 *
 * @param memberId 成员关系 ID
 * @param userId 用户 ID
 * @param nickname 昵称
 * @param mobile 手机号
 * @param role 角色
 * @param inviteStatus 邀请状态
 * @param joinedAt 加入时间
 */
public record FamilyMemberResponse(
    String memberId,
    String userId,
    String nickname,
    String mobile,
    String role,
    String inviteStatus,
    OffsetDateTime joinedAt
) {
}
