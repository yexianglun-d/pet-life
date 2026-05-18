package com.petlife.server.modules.home.dto.response;

/**
 * 首页设备摘要响应。
 *
 * @param enabled 是否已启用设备真实链路
 * @param title 标题
 * @param description 说明
 * @param alertCount 告警数量
 */
public record HomeDeviceSummaryResponse(
    Boolean enabled,
    String title,
    String description,
    Integer alertCount
) {
}
