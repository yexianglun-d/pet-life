package com.petlife.server.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 退出登录请求。
 *
 * @param refreshToken 当前会话对应的刷新令牌
 */
public record AuthLogoutRequest(
    @NotBlank(message = "刷新令牌不能为空")
    String refreshToken
) {
}
