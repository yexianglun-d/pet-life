package com.petlife.server.modules.notification.dto.response;

import java.util.List;

/**
 * 通知列表响应。
 *
 * @param items 通知列表
 * @param unreadCount 全部未读数
 * @param systemUnreadCount 系统通知未读数
 * @param reminderUnreadCount 提醒通知未读数
 */
public record NotificationListResponse(
    List<NotificationResponse> items,
    int unreadCount,
    int systemUnreadCount,
    int reminderUnreadCount
) {
}
