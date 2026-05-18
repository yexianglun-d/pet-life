package com.petlife.server.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户通知设置更新请求。
 *
 * @param notificationEnabled 通知总开关
 * @param privacyLevel 隐私级别
 */
public record UpdateUserNotificationSettingsRequest(
    @NotNull(message = "通知开关不能为空")
    Boolean notificationEnabled,
    @NotBlank(message = "隐私级别不能为空")
    String privacyLevel
) {
}
