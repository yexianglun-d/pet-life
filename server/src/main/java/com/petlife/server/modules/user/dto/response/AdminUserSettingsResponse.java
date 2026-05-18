package com.petlife.server.modules.user.dto.response;

/**
 * 后台用户设置响应。
 *
 * @param currentPetId 当前宠物 ID
 * @param notificationEnabled 通知总开关
 * @param privacyLevel 隐私级别
 */
public record AdminUserSettingsResponse(
    String currentPetId,
    boolean notificationEnabled,
    String privacyLevel
) {
}
