package com.petlife.server.modules.family.domain.entity;

import java.time.LocalDateTime;

/**
 * 家庭成员实体。
 */
public final class FamilyMemberEntity {

    private final Long memberId;
    private final Long familyId;
    private final Long userId;
    private final String nickname;
    private final String mobile;
    private final String role;
    private final String inviteStatus;
    private final LocalDateTime joinedAt;

    public FamilyMemberEntity(
        Long memberId,
        Long familyId,
        Long userId,
        String nickname,
        String mobile,
        String role,
        String inviteStatus,
        LocalDateTime joinedAt
    ) {
        this.memberId = memberId;
        this.familyId = familyId;
        this.userId = userId;
        this.nickname = nickname;
        this.mobile = mobile;
        this.role = role;
        this.inviteStatus = inviteStatus;
        this.joinedAt = joinedAt;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getMobile() {
        return mobile;
    }

    public String getRole() {
        return role;
    }

    public String getInviteStatus() {
        return inviteStatus;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
