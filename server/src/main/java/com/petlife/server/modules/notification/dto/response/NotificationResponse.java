package com.petlife.server.modules.notification.dto.response;

import java.time.OffsetDateTime;

/**
 * 通知响应。
 *
 * @param notificationId 通知 ID
 * @param notifyType 通知类型
 * @param bizType 业务类型
 * @param bizId 业务 ID
 * @param title 标题
 * @param content 内容
 * @param read 是否已读
 * @param sentAt 发送时间
 * @param readAt 阅读时间
 */
public record NotificationResponse(
    String notificationId,
    String notifyType,
    String bizType,
    String bizId,
    String title,
    String content,
    boolean read,
    OffsetDateTime sentAt,
    OffsetDateTime readAt
) {
}
