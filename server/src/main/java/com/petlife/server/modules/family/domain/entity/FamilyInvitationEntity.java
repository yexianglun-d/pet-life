package com.petlife.server.modules.family.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 家庭邀请实体。
 */
public final class FamilyInvitationEntity {

    private final Long invitationId;
    private final Long familyId;
    private final Long inviterUserId;
    private final String inviteeMobile;
    private final Long inviteeUserId;
    private final String role;
    private final List<Long> sharedPetIds;
    private final String inviteCode;
    private final String status;
    private final LocalDateTime expiredAt;
    private final LocalDateTime acceptedAt;
    private final LocalDateTime createdAt;

    public FamilyInvitationEntity(
        Long invitationId,
        Long familyId,
        Long inviterUserId,
        String inviteeMobile,
        Long inviteeUserId,
        String role,
        List<Long> sharedPetIds,
        String inviteCode,
        String status,
        LocalDateTime expiredAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt
    ) {
        this.invitationId = invitationId;
        this.familyId = familyId;
        this.inviterUserId = inviterUserId;
        this.inviteeMobile = inviteeMobile;
        this.inviteeUserId = inviteeUserId;
        this.role = role;
        this.sharedPetIds = sharedPetIds;
        this.inviteCode = inviteCode;
        this.status = status;
        this.expiredAt = expiredAt;
        this.acceptedAt = acceptedAt;
        this.createdAt = createdAt;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public String getInviteeMobile() {
        return inviteeMobile;
    }

    public Long getInviteeUserId() {
        return inviteeUserId;
    }

    public String getRole() {
        return role;
    }

    public List<Long> getSharedPetIds() {
        return sharedPetIds;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
