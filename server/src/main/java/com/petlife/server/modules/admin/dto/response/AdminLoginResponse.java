package com.petlife.server.modules.admin.dto.response;

/**
 * 后台登录响应。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param admin 当前管理员
 */
public record AdminLoginResponse(
    String accessToken,
    String refreshToken,
    AdminAccountResponse admin
) {
}
