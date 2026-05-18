package com.petlife.server.modules.reminder.dto.response;

/**
 * 后台提醒来源记录响应。
 *
 * @param sourceRecordId 来源健康记录 ID
 * @param recordType 来源健康记录类型
 * @param title 来源健康记录标题
 * @param status 来源记录状态：active/deleted/missing
 */
public record AdminReminderSourceResponse(
    String sourceRecordId,
    String recordType,
    String title,
    String status
) {
}
