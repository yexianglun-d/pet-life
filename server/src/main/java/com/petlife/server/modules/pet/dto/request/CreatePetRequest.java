package com.petlife.server.modules.pet.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 宠物创建请求。
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
public record CreatePetRequest(
    @NotBlank(message = "宠物名称不能为空")
    String petName,
    @NotBlank(message = "宠物类型不能为空")
    String petType,
    @NotBlank(message = "品种不能为空")
    String breed,
    @NotBlank(message = "性别不能为空")
    String gender,
    LocalDate birthday,
    LocalDate adoptDate,
    String neuterStatus,
    String avatarAssetId
) {
}
