package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 通知渠道配置持久化读模型。
 */
public record NotificationChannelConfigDataObject(
    Long channelConfigId,
    String channelType,
    String providerCode,
    String providerName,
    Boolean enabled,
    String configStatus,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
