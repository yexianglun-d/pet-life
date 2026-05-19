package com.petlife.server.modules.notification.dto.response;

import java.time.LocalDateTime;

/**
 * 消息模板响应。
 *
 * @param templateId 模板 ID
 * @param templateCode 模板编码
 * @param channelType 渠道类型
 * @param titleTemplate 标题模板
 * @param contentTemplate 内容模板
 * @param enabled 是否启用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record MessageTemplateResponse(
    String templateId,
    String templateCode,
    String channelType,
    String titleTemplate,
    String contentTemplate,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
