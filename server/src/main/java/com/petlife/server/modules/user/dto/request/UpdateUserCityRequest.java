package com.petlife.server.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户城市更新请求。
 *
 * @param cityCode 城市编码
 * @param cityName 城市名称
 */
public record UpdateUserCityRequest(
    @NotBlank(message = "城市编码不能为空")
    @Size(max = 32, message = "城市编码长度不能超过 32 个字符")
    String cityCode,
    @NotBlank(message = "城市名称不能为空")
    @Size(max = 50, message = "城市名称长度不能超过 50 个字符")
    String cityName
) {
}
