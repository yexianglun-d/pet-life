package com.petlife.server.modules.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台登录请求。
 *
 * @param username 后台账号
 * @param password 登录密码
 */
public record AdminLoginRequest(
    @NotBlank(message = "后台账号不能为空")
    @Size(max = 50, message = "后台账号长度不能超过 50 个字符")
    String username,

    @NotBlank(message = "后台密码不能为空")
    @Size(max = 100, message = "后台密码长度不能超过 100 个字符")
    String password
) {
}
