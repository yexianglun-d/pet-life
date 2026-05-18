package com.petlife.server.modules.reminder.dto.response;

import java.time.LocalDateTime;

/**
 * 后台提醒模板响应。
 *
 * @param templateId 模板 ID
 * @param templateName 模板名称
 * @param reminderType 提醒类型
 * @param defaultReminderMode 默认提醒模式
 * @param defaultAdvanceValue 默认提前量
 * @param defaultAdvanceUnit 默认提前单位
 * @param defaultCycleValue 默认周期值
 * @param defaultCycleUnit 默认周期单位
 * @param applicablePetType 适用宠物类型
 * @param enabled 是否启用
 * @param sortOrder 展示排序
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ReminderTemplateResponse(
    String templateId,
    String templateName,
    String reminderType,
    String defaultReminderMode,
    Integer defaultAdvanceValue,
    String defaultAdvanceUnit,
    Integer defaultCycleValue,
    String defaultCycleUnit,
    String applicablePetType,
    boolean enabled,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
