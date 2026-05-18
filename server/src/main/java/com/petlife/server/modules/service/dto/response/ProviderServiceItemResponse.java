package com.petlife.server.modules.service.dto.response;

import java.math.BigDecimal;

/**
 * 服务商服务项目响应。
 *
 * @param serviceItemId 服务项目 ID
 * @param serviceCode 服务编码
 * @param serviceName 服务名称
 * @param serviceDesc 服务说明
 * @param priceMin 最低价格
 * @param priceMax 最高价格
 * @param status 状态
 */
public record ProviderServiceItemResponse(
    String serviceItemId,
    String serviceCode,
    String serviceName,
    String serviceDesc,
    BigDecimal priceMin,
    BigDecimal priceMax,
    String status
) {
}
