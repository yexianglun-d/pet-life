package com.petlife.server.modules.pet.persistence.dataobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 后台宠物查询数据对象。
 */
public record AdminPetDataObject(
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
    BigDecimal weightKg,
    String allergyNotes,
    String medicalHistory,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String familyName,
    Integer familyStatus,
    Integer familyMemberCount,
    String ownerNickname,
    String ownerMobile
) {
}
