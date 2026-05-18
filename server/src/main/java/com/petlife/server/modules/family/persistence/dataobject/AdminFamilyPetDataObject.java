package com.petlife.server.modules.family.persistence.dataobject;

/**
 * 后台家庭宠物数据对象。
 */
public record AdminFamilyPetDataObject(
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
}
