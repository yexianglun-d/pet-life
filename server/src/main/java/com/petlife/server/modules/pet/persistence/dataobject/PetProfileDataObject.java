package com.petlife.server.modules.pet.persistence.dataobject;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物主档数据对象。
 *
 * @param petId 宠物 ID
 * @param familyId 家庭 ID
 * @param ownerUserId 主要拥有者用户 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 * @param gender 性别
 * @param birthday 出生日期
 * @param adoptDate 到家日期
 * @param neuterStatus 绝育状态：0-否 1-是
 * @param avatarUrl 头像地址
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record PetProfileDataObject(
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
}
