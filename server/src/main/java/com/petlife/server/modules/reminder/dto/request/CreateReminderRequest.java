package com.petlife.server.modules.reminder.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/**
 * 提醒创建请求。
 *
 * @param reminderType 提醒类型
 * @param title 标题
 * @param dueAt 到期时间
 * @param notes 备注
 */
public record CreateReminderRequest(
    @NotBlank(message = "提醒类型不能为空")
    String reminderType,
    @NotBlank(message = "提醒标题不能为空")
    String title,
    OffsetDateTime dueAt,
    String notes
) {
}
