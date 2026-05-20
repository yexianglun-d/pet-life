package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 后台维护服务商坐标请求。
 */
public record AdminUpdateProviderLocationRequest(
    @Size(max = 255, message = "地址长度不能超过 255 个字符")
    String address,
    @NotNull(message = "纬度不能为空")
    BigDecimal latitude,
    @NotNull(message = "经度不能为空")
    BigDecimal longitude,
    @Size(max = 20, message = "坐标来源长度不能超过 20 个字符")
    String coordinateSource
) {
}
