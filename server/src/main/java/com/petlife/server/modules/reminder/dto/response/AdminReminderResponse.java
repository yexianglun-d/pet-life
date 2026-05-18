package com.petlife.server.modules.reminder.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;
import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;

/**
 * 后台提醒响应。
 *
 * @param reminder 提醒详情
 * @param pet 宠物归属上下文
 * @param handler 最近处理人
 * @param sourceRecord 来源健康记录
 */
public record AdminReminderResponse(
    ReminderResponse reminder,
    AdminPetContextResponse pet,
    AdminUserContextResponse handler,
    AdminReminderSourceResponse sourceRecord
) {
}
