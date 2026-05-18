package com.petlife.server.modules.admin.dto.response;

import java.time.OffsetDateTime;

/**
 * 后台管理员账号响应。
 *
 * @param adminAccountId 管理员账号 ID
 * @param username 登录账号
 * @param displayName 展示名称
 * @param roleCode 角色编码
 * @param status 账号状态
 * @param lastLoginAt 最近登录时间
 * @param createdAt 创建时间
 */
public record AdminAccountResponse(
    String adminAccountId,
    String username,
    String displayName,
    String roleCode,
    Integer status,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {
}
