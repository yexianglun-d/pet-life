package com.petlife.server.modules.home.dto.response;

/**
 * 首页服务入口响应。
 *
 * @param entryKey 入口编码
 * @param label 展示名称
 * @param description 说明
 * @param enabled 当前是否可进入真实服务链路
 */
public record HomeServiceEntryResponse(
    String entryKey,
    String label,
    String description,
    Boolean enabled
) {
}
