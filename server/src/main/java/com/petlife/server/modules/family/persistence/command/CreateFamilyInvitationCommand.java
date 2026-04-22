package com.petlife.server.modules.family.persistence.command;

import java.time.LocalDateTime;

/**
 * 创建家庭邀请命令。
 */
public class CreateFamilyInvitationCommand {

    private Long id;
    private Long familyId;
    private Long inviterUserId;
    private String inviteeMobile;
    private Long inviteeUserId;
    private String role;
    private String sharedPetIdsJson;
    private String inviteCode;
    private LocalDateTime expiredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(Long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public String getInviteeMobile() {
        return inviteeMobile;
    }

    public void setInviteeMobile(String inviteeMobile) {
        this.inviteeMobile = inviteeMobile;
    }

    public Long getInviteeUserId() {
        return inviteeUserId;
    }

    public void setInviteeUserId(Long inviteeUserId) {
        this.inviteeUserId = inviteeUserId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSharedPetIdsJson() {
        return sharedPetIdsJson;
    }

    public void setSharedPetIdsJson(String sharedPetIdsJson) {
        this.sharedPetIdsJson = sharedPetIdsJson;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
