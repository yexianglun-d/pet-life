package com.petlife.server.modules.user.dto.response;

/**
 * 用户设置响应。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param currentPetId 当前宠物 ID
 * @param notificationEnabled 通知总开关
 * @param privacyLevel 隐私级别
 */
public record UserSettingsResponse(
    String userId,
    String mobile,
    String nickname,
    String cityCode,
    String cityName,
    String currentPetId,
    boolean notificationEnabled,
    String privacyLevel
) {
}
