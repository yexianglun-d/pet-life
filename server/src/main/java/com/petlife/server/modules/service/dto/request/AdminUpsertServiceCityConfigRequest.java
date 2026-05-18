package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台服务城市开通配置请求。
 *
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param opened 是否开通
 * @param unavailableReason 未开通原因
 * @param sortOrder 展示排序
 */
public record AdminUpsertServiceCityConfigRequest(
    @NotBlank(message = "城市编码不能为空")
    @Size(max = 32, message = "城市编码不能超过 32 个字符")
    String cityCode,

    @NotBlank(message = "城市名称不能为空")
    @Size(max = 50, message = "城市名称不能超过 50 个字符")
    String cityName,

    Boolean opened,

    @Size(max = 255, message = "未开通原因不能超过 255 个字符")
    String unavailableReason,

    Integer sortOrder
) {
}
