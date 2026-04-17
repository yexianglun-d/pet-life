package com.petlife.server.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 验证码发送请求。
 *
 * @param mobile 手机号
 * @param scene 业务场景
 */
public record AuthSmsSendRequest(
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    String mobile,

    @NotBlank(message = "业务场景不能为空")
    String scene
) {
}
