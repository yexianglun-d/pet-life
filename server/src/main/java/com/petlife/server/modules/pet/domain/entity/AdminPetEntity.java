package com.petlife.server.modules.pet.domain.entity;

/**
 * 后台宠物聚合实体。
 */
public final class AdminPetEntity {

    private final PetProfileEntity petProfile;
    private final String familyName;
    private final Integer familyStatus;
    private final Integer familyMemberCount;
    private final String ownerNickname;
    private final String ownerMobile;

    public AdminPetEntity(
        PetProfileEntity petProfile,
        String familyName,
        Integer familyStatus,
        Integer familyMemberCount,
        String ownerNickname,
        String ownerMobile
    ) {
        this.petProfile = petProfile;
        this.familyName = familyName;
        this.familyStatus = familyStatus;
        this.familyMemberCount = familyMemberCount;
        this.ownerNickname = ownerNickname;
        this.ownerMobile = ownerMobile;
    }

    public PetProfileEntity getPetProfile() {
        return petProfile;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Integer getFamilyStatus() {
        return familyStatus;
    }

    public Integer getFamilyMemberCount() {
        return familyMemberCount;
    }

    public String getOwnerNickname() {
        return ownerNickname;
    }

    public String getOwnerMobile() {
        return ownerMobile;
    }
}
