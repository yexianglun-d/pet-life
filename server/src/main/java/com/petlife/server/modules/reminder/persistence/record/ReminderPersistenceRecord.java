package com.petlife.server.modules.reminder.persistence.record;

import java.time.LocalDateTime;

/**
 * 提醒持久化记录。
 *
 * @param reminderId 提醒 ID
 * @param petId 宠物 ID
 * @param reminderType 提醒类型
 * @param title 提醒标题
 * @param dueAt 提醒时间
 * @param status 状态
 * @param notes 备注
 * @param handledAt 处理时间
 * @param createdAt 创建时间
 */
public record ReminderPersistenceRecord(
    Long reminderId,
    Long petId,
    String reminderType,
    String title,
    LocalDateTime dueAt,
    String status,
    String notes,
    LocalDateTime handledAt,
    LocalDateTime createdAt
) {
}
