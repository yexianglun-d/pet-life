package com.petlife.server.modules.home.dto.response;

/**
 * 首页快捷记录动作响应。
 *
 * @param actionKey 动作编码
 * @param label 展示名称
 * @param targetType 写入目标类型
 * @param description 引导说明
 */
public record HomeQuickActionResponse(
    String actionKey,
    String label,
    String targetType,
    String description
) {
}
