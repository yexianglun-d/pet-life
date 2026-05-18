package com.petlife.server.modules.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台刷新登录态请求。
 *
 * @param refreshToken 刷新令牌
 */
public record AdminRefreshTokenRequest(
    @NotBlank(message = "刷新令牌不能为空")
    String refreshToken
) {
}
