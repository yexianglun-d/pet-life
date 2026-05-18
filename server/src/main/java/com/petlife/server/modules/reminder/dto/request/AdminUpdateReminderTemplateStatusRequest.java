package com.petlife.server.modules.reminder.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 后台提醒模板启停请求。
 *
 * @param enabled 是否启用
 */
public record AdminUpdateReminderTemplateStatusRequest(
    @NotNull(message = "启用状态不能为空")
    Boolean enabled
) {
}
