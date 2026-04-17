package com.petlife.server.modules.reminder.dto.response;

import java.time.OffsetDateTime;

/**
 * 提醒响应。
 *
 * @param reminderId 提醒 ID
 * @param petId 宠物 ID
 * @param reminderType 提醒类型
 * @param title 标题
 * @param dueAt 到期时间
 * @param status 状态
 * @param notes 备注
 * @param completedAt 完成时间
 * @param createdAt 创建时间
 */
public record ReminderResponse(
    String reminderId,
    String petId,
    String reminderType,
    String title,
    OffsetDateTime dueAt,
    String status,
    String notes,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt
) {
}
