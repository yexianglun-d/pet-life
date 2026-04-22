package com.petlife.server.modules.reminder.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 提醒数据对象。
 *
 * @param reminderId 提醒 ID
 * @param petId 宠物 ID
 * @param reminderType 提醒类型
 * @param title 提醒标题
 * @param reminderMode 提醒模式
 * @param cycleValue 周期间隔值
 * @param cycleUnit 周期单位
 * @param dueAt 提醒时间
 * @param status 状态
 * @param notes 备注
 * @param handledAt 处理时间
 * @param createdAt 创建时间
 */
public record ReminderDataObject(
    Long reminderId,
    Long petId,
    String reminderType,
    String title,
    String reminderMode,
    Integer cycleValue,
    String cycleUnit,
    LocalDateTime dueAt,
    String status,
    String notes,
    LocalDateTime handledAt,
    LocalDateTime createdAt
) {
}
