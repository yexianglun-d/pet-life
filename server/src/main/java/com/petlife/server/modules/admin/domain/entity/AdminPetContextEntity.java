package com.petlife.server.modules.admin.domain.entity;

/**
 * 后台宠物上下文实体。
 *
 * <p>后台治理列表需要同时展示业务记录和归属关系，该实体只承载只读上下文，
 * 不参与宠物主档状态修改。</p>
 */
public final class AdminPetContextEntity {

    private final Long petId;
    private final String petName;
    private final String petType;
    private final Long familyId;
    private final String familyName;
    private final Long ownerUserId;
    private final String ownerNickname;
    private final String ownerMobile;

    public AdminPetContextEntity(
        Long petId,
        String petName,
        String petType,
        Long familyId,
        String familyName,
        Long ownerUserId,
        String ownerNickname,
        String ownerMobile
    ) {
        this.petId = petId;
        this.petName = petName;
        this.petType = petType;
        this.familyId = familyId;
        this.familyName = familyName;
        this.ownerUserId = ownerUserId;
        this.ownerNickname = ownerNickname;
        this.ownerMobile = ownerMobile;
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
}
