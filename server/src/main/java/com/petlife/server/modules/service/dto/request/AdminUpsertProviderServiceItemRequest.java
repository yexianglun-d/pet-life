package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 后台新增或编辑服务项目请求。
 */
public record AdminUpsertProviderServiceItemRequest(
    @NotBlank(message = "服务编码不能为空")
    @Size(max = 30, message = "服务编码长度不能超过 30 个字符")
    String serviceCode,
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称长度不能超过 100 个字符")
    String serviceName,
    @Size(max = 500, message = "服务说明长度不能超过 500 个字符")
    String serviceDesc,
    BigDecimal priceMin,
    BigDecimal priceMax,
    @NotBlank(message = "服务项目状态不能为空")
    String status
) {
}
