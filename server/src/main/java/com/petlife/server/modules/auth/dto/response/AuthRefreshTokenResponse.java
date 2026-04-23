package com.petlife.server.modules.auth.dto.response;

/**
 * 刷新登录态响应。
 *
 * @param accessToken 新访问令牌
 * @param refreshToken 新刷新令牌
 */
public record AuthRefreshTokenResponse(
    String accessToken,
    String refreshToken
) {
}
