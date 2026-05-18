package com.petlife.server.modules.family.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台家庭聚合实体。
 */
public final class AdminFamilyEntity {

    private final Long familyId;
    private final String familyName;
    private final Long ownerUserId;
    private final String ownerNickname;
    private final String ownerMobile;
    private final Integer status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Integer memberCount;
    private final Integer petCount;
    private final List<FamilyMemberEntity> members;
    private final List<AdminFamilyPetEntity> pets;

    public AdminFamilyEntity(
        Long familyId,
        String familyName,
        Long ownerUserId,
        String ownerNickname,
        String ownerMobile,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer memberCount,
        Integer petCount,
        List<FamilyMemberEntity> members,
        List<AdminFamilyPetEntity> pets
    ) {
        this.familyId = familyId;
        this.familyName = familyName;
        this.ownerUserId = ownerUserId;
        this.ownerNickname = ownerNickname;
        this.ownerMobile = ownerMobile;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.memberCount = memberCount;
        this.petCount = petCount;
        this.members = members;
        this.pets = pets;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerNickname() {
        return ownerNickname;
    }

    public String getOwnerMobile() {
        return ownerMobile;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public Integer getPetCount() {
        return petCount;
    }

    public List<FamilyMemberEntity> getMembers() {
        return members;
    }

    public List<AdminFamilyPetEntity> getPets() {
        return pets;
    }
}
