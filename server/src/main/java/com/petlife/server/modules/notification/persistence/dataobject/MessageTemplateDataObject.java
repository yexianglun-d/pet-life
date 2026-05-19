package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 消息模板持久化读模型。
 */
public record MessageTemplateDataObject(
    Long templateId,
    String templateCode,
    String channelType,
    String titleTemplate,
    String contentTemplate,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
