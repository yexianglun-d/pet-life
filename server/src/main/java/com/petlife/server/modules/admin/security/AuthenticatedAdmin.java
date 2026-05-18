package com.petlife.server.modules.admin.security;

/**
 * 已通过后台会话认证的管理员身份。
 *
 * @param adminAccountId 管理员账号 ID
 * @param username 登录账号
 * @param displayName 展示名称
 * @param roleCode 角色编码
 */
public record AuthenticatedAdmin(
    Long adminAccountId,
    String username,
    String displayName,
    String roleCode
) {
}
