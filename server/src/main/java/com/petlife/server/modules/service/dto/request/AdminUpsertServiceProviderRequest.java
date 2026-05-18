package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 后台新增或编辑服务商请求。
 */
public record AdminUpsertServiceProviderRequest(
    @NotBlank(message = "服务商类型不能为空")
    String providerType,
    @NotBlank(message = "服务商名称不能为空")
    @Size(max = 100, message = "服务商名称长度不能超过 100 个字符")
    String providerName,
    @NotBlank(message = "城市编码不能为空")
    @Size(max = 32, message = "城市编码长度不能超过 32 个字符")
    String cityCode,
    @Size(max = 255, message = "地址长度不能超过 255 个字符")
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    String contactPhone,
    @Size(max = 255, message = "营业时间长度不能超过 255 个字符")
    String businessHours,
    BigDecimal ratingAvg,
    Integer reviewCount,
    @NotBlank(message = "服务商状态不能为空")
    String status
) {
}
