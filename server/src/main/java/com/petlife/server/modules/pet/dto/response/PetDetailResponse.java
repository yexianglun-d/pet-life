package com.petlife.server.modules.pet.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 宠物详情响应。
 *
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param petType 宠物类型
 * @param breed 品种
 * @param gender 性别
 * @param birthday 生日
 * @param adoptDate 到家日期
 * @param neuterStatus 绝育状态
 * @param avatarUrl 头像地址
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record PetDetailResponse(
    String petId,
    String petName,
    String petType,
    String breed,
    String gender,
    LocalDate birthday,
    LocalDate adoptDate,
    String neuterStatus,
    String avatarUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
