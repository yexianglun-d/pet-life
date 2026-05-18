package com.petlife.server.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户资料更新请求。
 *
 * @param nickname 用户昵称
 */
public record UpdateUserProfileRequest(
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    String nickname
) {
}
