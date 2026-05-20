package com.petlife.server.modules.location.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台地理编码辅助请求。
 */
public record AdminGeocodeRequest(
    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址长度不能超过 255 个字符")
    String address,
    @Size(max = 32, message = "城市编码长度不能超过 32 个字符")
    String city
) {
}
