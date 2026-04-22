package com.petlife.server.modules.reminder.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/**
 * 提醒创建请求。
 *
 * @param reminderType 提醒类型
 * @param title 标题
 * @param reminderMode 提醒模式
 * @param cycleValue 周期间隔值
 * @param cycleUnit 周期单位
 * @param dueAt 到期时间
 * @param notes 备注
 */
public record CreateReminderRequest(
    @NotBlank(message = "提醒类型不能为空")
    String reminderType,
    @NotBlank(message = "提醒标题不能为空")
    String title,
    String reminderMode,
    Integer cycleValue,
    String cycleUnit,
    OffsetDateTime dueAt,
    String notes
) {
}
