package com.petlife.server.modules.dailylog.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;
import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;

/**
 * 后台萌宠日常响应。
 *
 * @param dailyLog 萌宠日常
 * @param pet 宠物归属上下文
 * @param author 作者用户
 */
public record AdminDailyLogResponse(
    DailyLogResponse dailyLog,
    AdminPetContextResponse pet,
    AdminUserContextResponse author
) {
}
