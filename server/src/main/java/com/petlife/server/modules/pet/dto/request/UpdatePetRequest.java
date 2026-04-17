package com.petlife.server.modules.pet.dto.request;

import java.time.LocalDate;

/**
 * 宠物更新请求。
 *
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 * @param gender 性别
 * @param birthday 生日
 * @param adoptDate 到家日期
 * @param neuterStatus 绝育状态
 * @param avatarAssetId 头像资源 ID
 */
public record UpdatePetRequest(
    String petName,
    String petType,
    String breed,
    String gender,
    LocalDate birthday,
    LocalDate adoptDate,
    String neuterStatus,
    String avatarAssetId
) {
}
