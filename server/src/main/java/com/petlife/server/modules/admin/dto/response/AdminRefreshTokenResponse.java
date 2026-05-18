package com.petlife.server.modules.admin.dto.response;

/**
 * 后台刷新登录态响应。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 */
public record AdminRefreshTokenResponse(
    String accessToken,
    String refreshToken
) {
}
