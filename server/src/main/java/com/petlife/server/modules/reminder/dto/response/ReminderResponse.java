package com.petlife.server.modules.reminder.dto.response;

import java.time.OffsetDateTime;

/**
 * 提醒响应。
 *
 * @param reminderId 提醒 ID
 * @param petId 宠物 ID
 * @param reminderType 提醒类型
 * @param title 标题
 * @param reminderMode 提醒模式
 * @param cycleValue 周期间隔值
 * @param cycleUnit 周期单位
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
    String reminderMode,
    Integer cycleValue,
    String cycleUnit,
    OffsetDateTime dueAt,
    String status,
    String notes,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt
) {
}
