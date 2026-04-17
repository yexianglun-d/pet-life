package com.petlife.server.modules.auth.token;

/**
 * 登录令牌对。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 */
public record IssuedLoginTokens(
    String accessToken,
    String refreshToken
) {
}
