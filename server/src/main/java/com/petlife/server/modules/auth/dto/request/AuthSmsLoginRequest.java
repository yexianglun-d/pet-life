package com.petlife.server.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 短信登录请求。
 *
 * @param mobile 手机号
 * @param code 短信验证码
 */
public record AuthSmsLoginRequest(
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    String mobile,

    @NotBlank(message = "验证码不能为空")
    String code
) {
}
