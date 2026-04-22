package com.petlife.server.modules.pet.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物主档实体。
 *
 * <p>该实体沉淀宠物主档的稳定业务属性，
 * 供宠物详情、当前宠物摘要和多模块聚合场景复用。</p>
 */
public final class PetProfileEntity {

    private final Long petId;
    private final Long familyId;
    private final Long ownerUserId;
    private final String petName;
    private final String petType;
    private final String breed;
    private final String gender;
    private final LocalDate birthday;
    private final LocalDate adoptDate;
    private final Integer neuterStatus;
    private final String avatarUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PetProfileEntity(
        Long petId,
        Long familyId,
        Long ownerUserId,
        String petName,
        String petType,
        String breed,
        String gender,
        LocalDate birthday,
        LocalDate adoptDate,
        Integer neuterStatus,
        String avatarUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.petId = petId;
        this.familyId = familyId;
        this.ownerUserId = ownerUserId;
        this.petName = petName;
        this.petType = petType;
        this.breed = breed;
        this.gender = gender;
        this.birthday = birthday;
        this.adoptDate = adoptDate;
        this.neuterStatus = neuterStatus;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getPetId() {
        return petId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
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

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public LocalDate getAdoptDate() {
        return adoptDate;
    }

    public Integer getNeuterStatus() {
        return neuterStatus;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
