package com.petlife.server.modules.family.domain.entity;

/**
 * 后台家庭宠物实体。
 */
public final class AdminFamilyPetEntity {

    private final Long petId;
    private final Long familyId;
    private final String petName;
    private final String petType;
    private final String breed;
    private final String status;
    private final Long ownerUserId;
    private final String ownerNickname;
    private final String ownerMobile;

    public AdminFamilyPetEntity(
        Long petId,
        Long familyId,
        String petName,
        String petType,
        String breed,
        String status,
        Long ownerUserId,
        String ownerNickname,
        String ownerMobile
    ) {
        this.petId = petId;
        this.familyId = familyId;
        this.petName = petName;
        this.petType = petType;
        this.breed = breed;
        this.status = status;
        this.ownerUserId = ownerUserId;
        this.ownerNickname = ownerNickname;
        this.ownerMobile = ownerMobile;
    }

    public Long getPetId() {
        return petId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getPetName() {
        return petName;
    }

    public String getPetType() {
        return petType;
    }

    public String getBreed() {
        return breed;
    }

    public String getStatus() {
        return status;
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
}
