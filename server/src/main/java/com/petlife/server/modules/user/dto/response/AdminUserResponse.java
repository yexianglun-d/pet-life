package com.petlife.server.modules.user.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;
import java.time.OffsetDateTime;

/**
 * 后台用户响应。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param avatarUrl 头像地址
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param status 账号状态
 * @param lastLoginAt 最近登录时间
 * @param createdAt 创建时间
 * @param settings 用户设置
 * @param primaryFamily 主要家庭上下文
 * @param currentPet 当前宠物上下文
 * @param petCount 用户可见活跃宠物数
 */
public record AdminUserResponse(
    String userId,
    String mobile,
    String nickname,
    String avatarUrl,
    String cityCode,
    String cityName,
    Integer status,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt,
    AdminUserSettingsResponse settings,
    AdminUserFamilyResponse primaryFamily,
    AdminPetContextResponse currentPet,
    Integer petCount
) {
}
