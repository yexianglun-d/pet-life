package com.petlife.server.modules.location.domain.entity;

import java.math.BigDecimal;

/**
 * 经纬度坐标。
 *
 * <p>高德 Web 服务按“经度,纬度”传参，服务端领域模型统一保留纬度、经度字段顺序，
 * 避免业务代码直接拼接供应商参数时写反坐标。</p>
 */
public record GeoPointEntity(
    BigDecimal latitude,
    BigDecimal longitude
) {
}
