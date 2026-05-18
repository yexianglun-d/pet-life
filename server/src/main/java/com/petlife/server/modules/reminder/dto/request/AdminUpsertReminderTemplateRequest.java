package com.petlife.server.modules.reminder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台创建或更新提醒模板请求。
 *
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
 */
public record AdminUpsertReminderTemplateRequest(
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过 100 个字符")
    String templateName,

    @NotBlank(message = "提醒类型不能为空")
    @Size(max = 30, message = "提醒类型不能超过 30 个字符")
    String reminderType,

    @NotBlank(message = "默认提醒模式不能为空")
    @Size(max = 20, message = "默认提醒模式不能超过 20 个字符")
    String defaultReminderMode,

    @NotNull(message = "默认提前量不能为空")
    Integer defaultAdvanceValue,

    @NotBlank(message = "默认提前单位不能为空")
    @Size(max = 20, message = "默认提前单位不能超过 20 个字符")
    String defaultAdvanceUnit,

    Integer defaultCycleValue,

    @Size(max = 20, message = "默认周期单位不能超过 20 个字符")
    String defaultCycleUnit,

    @NotBlank(message = "适用宠物类型不能为空")
    @Size(max = 20, message = "适用宠物类型不能超过 20 个字符")
    String applicablePetType,

    @NotNull(message = "启用状态不能为空")
    Boolean enabled,

    Integer sortOrder
) {
}
