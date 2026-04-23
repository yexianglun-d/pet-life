package com.petlife.server.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新登录态请求。
 *
 * @param refreshToken 刷新令牌
 */
public record AuthRefreshTokenRequest(
    @NotBlank(message = "刷新令牌不能为空")
    String refreshToken
) {
}
