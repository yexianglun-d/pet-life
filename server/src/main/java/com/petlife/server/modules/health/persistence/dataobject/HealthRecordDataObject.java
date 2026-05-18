package com.petlife.server.modules.health.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 健康记录数据对象。
 *
 * @param healthRecordId 健康记录 ID
 * @param petId 宠物 ID
 * @param operatorUserId 操作人用户 ID
 * @param recordType 记录类型
 * @param title 记录标题
 * @param occurredAt 发生时间
 * @param hospitalName 医院名称
 * @param doctorName 医生名称
 * @param severityLevel 严重程度
 * @param resultSummary 结果摘要
 * @param attachments 附件 JSON
 * @param nextReminderId 自动生成的下一次提醒 ID
 * @param nextReminderAt 自动生成的下一次提醒时间
 * @param nextReminderStatus 自动生成的下一次提醒状态
 * @param notes 备注
 * @param createdAt 创建时间
 */
public record HealthRecordDataObject(
    Long healthRecordId,
    Long petId,
    Long operatorUserId,
    String recordType,
    String title,
    LocalDateTime occurredAt,
    String hospitalName,
    String doctorName,
    String severityLevel,
    String resultSummary,
    String attachments,
    Long nextReminderId,
    LocalDateTime nextReminderAt,
    String nextReminderStatus,
    String notes,
    LocalDateTime createdAt
) {
}
