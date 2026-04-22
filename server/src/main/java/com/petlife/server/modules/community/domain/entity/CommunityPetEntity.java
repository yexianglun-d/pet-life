package com.petlife.server.modules.community.domain.entity;

/**
 * 社区帖子关联宠物实体。
 */
public final class CommunityPetEntity {

    private final Long petId;
    private final String petName;
    private final String petType;
    private final String breed;

    public CommunityPetEntity(Long petId, String petName, String petType, String breed) {
        this.petId = petId;
        this.petName = petName;
        this.petType = petType;
        this.breed = breed;
    }

    public Long getPetId() {
        return petId;
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
}
