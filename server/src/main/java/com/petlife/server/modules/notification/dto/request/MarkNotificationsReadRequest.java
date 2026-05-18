package com.petlife.server.modules.notification.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 批量已读请求。
 *
 * @param notifyType 通知类型，缺省或 all 表示全部
 */
public record MarkNotificationsReadRequest(
    @Size(max = 30, message = "通知类型长度不能超过 30 个字符")
    String notifyType
) {
}
