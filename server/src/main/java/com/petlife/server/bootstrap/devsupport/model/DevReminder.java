package com.petlife.server.bootstrap.devsupport.model;

import java.time.OffsetDateTime;

/**
 * 开发期提醒模型。
 *
 * @param reminderId 提醒 ID
 * @param petId 宠物 ID
 * @param reminderType 提醒类型
 * @param title 提醒标题
 * @param dueAt 到期时间
 * @param status 状态
 * @param notes 备注
 * @param completedAt 完成时间
 * @param createdAt 创建时间
 */
public record DevReminder(
    Long reminderId,
    Long petId,
    String reminderType,
    String title,
    OffsetDateTime dueAt,
    String status,
    String notes,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt
) {
}
