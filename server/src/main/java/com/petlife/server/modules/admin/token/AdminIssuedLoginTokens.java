package com.petlife.server.modules.admin.token;

/**
 * 后台登录令牌对。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 */
public record AdminIssuedLoginTokens(
    String accessToken,
    String refreshToken
) {
}
