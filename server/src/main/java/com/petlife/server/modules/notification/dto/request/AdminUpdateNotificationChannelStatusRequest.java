package com.petlife.server.modules.notification.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 后台通知渠道启停请求。
 *
 * @param enabled 是否启用
 */
public record AdminUpdateNotificationChannelStatusRequest(
    @NotNull(message = "启用状态不能为空")
    Boolean enabled
) {
}
