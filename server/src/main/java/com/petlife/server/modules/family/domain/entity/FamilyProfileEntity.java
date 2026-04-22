package com.petlife.server.modules.family.domain.entity;

/**
 * 家庭档案实体。
 */
public final class FamilyProfileEntity {

    private final Long familyId;
    private final String familyName;
    private final Long ownerUserId;
    private final Integer memberCount;
    private final String currentUserRole;

    public FamilyProfileEntity(
        Long familyId,
        String familyName,
        Long ownerUserId,
        Integer memberCount,
        String currentUserRole
    ) {
        this.familyId = familyId;
        this.familyName = familyName;
        this.ownerUserId = ownerUserId;
        this.memberCount = memberCount;
        this.currentUserRole = currentUserRole;
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

    public Integer getMemberCount() {
        return memberCount;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }
}
