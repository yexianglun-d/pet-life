package com.petlife.server.modules.notification.dto.response;

import java.time.LocalDateTime;

/**
 * 通知渠道配置响应。
 *
 * @param channelConfigId 配置 ID
 * @param channelType 渠道类型
 * @param providerCode 供应商编码
 * @param providerName 供应商名称
 * @param enabled 是否启用
 * @param configStatus 配置状态
 * @param remark 备注
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record NotificationChannelConfigResponse(
    String channelConfigId,
    String channelType,
    String providerCode,
    String providerName,
    boolean enabled,
    String configStatus,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
