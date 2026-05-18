package com.petlife.server.modules.health.dto.response;

import com.petlife.server.modules.admin.dto.response.AdminPetContextResponse;
import com.petlife.server.modules.admin.dto.response.AdminUserContextResponse;

/**
 * 后台健康记录响应。
 *
 * @param healthRecord 健康记录
 * @param pet 宠物归属上下文
 * @param operator 记录操作者
 */
public record AdminHealthRecordResponse(
    HealthRecordResponse healthRecord,
    AdminPetContextResponse pet,
    AdminUserContextResponse operator
) {
}
