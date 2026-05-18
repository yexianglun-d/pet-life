package com.petlife.server.modules.service.dto.response;

import java.time.OffsetDateTime;

/**
 * 服务城市开通配置响应。
 *
 * @param configId 配置 ID
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param opened 是否开通
 * @param unavailableReason 未开通原因
 * @param sortOrder 展示排序
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ServiceCityConfigResponse(
    String configId,
    String cityCode,
    String cityName,
    boolean opened,
    String unavailableReason,
    Integer sortOrder,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
