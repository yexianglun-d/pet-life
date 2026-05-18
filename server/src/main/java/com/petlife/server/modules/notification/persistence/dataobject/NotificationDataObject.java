package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 通知数据对象。
 *
 * @param notificationId 通知 ID
 * @param userId 接收用户 ID
 * @param notifyType 通知类型
 * @param bizType 业务类型
 * @param bizId 业务 ID
 * @param title 标题
 * @param content 内容
 * @param readStatus 已读状态
 * @param sentAt 发送时间
 * @param readAt 阅读时间
 */
public record NotificationDataObject(
    Long notificationId,
    Long userId,
    String notifyType,
    String bizType,
    Long bizId,
    String title,
    String content,
    Integer readStatus,
    LocalDateTime sentAt,
    LocalDateTime readAt
) {
}
