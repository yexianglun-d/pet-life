package com.petlife.server.modules.health.dto.response;

import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 健康记录响应。
 *
 * @param healthRecordId 健康记录 ID
 * @param petId 宠物 ID
 * @param recordType 记录类型
 * @param title 标题
 * @param value 数值
 * @param unit 单位
 * @param hospitalName 医院名称
 * @param doctorName 医生名称
 * @param severityLevel 严重程度
 * @param resultSummary 结果摘要
 * @param attachmentAssetIds 附件资产 ID
 * @param attachmentAssets 附件资产元数据
 * @param nextReminderId 自动生成的下一次提醒 ID
 * @param nextReminderAt 自动生成的下一次提醒时间
 * @param nextReminderStatus 自动生成的下一次提醒状态
 * @param occurredAt 发生时间
 * @param notes 备注
 * @param createdAt 创建时间
 */
public record HealthRecordResponse(
    String healthRecordId,
    String petId,
    String recordType,
    String title,
    String value,
    String unit,
    String hospitalName,
    String doctorName,
    String severityLevel,
    String resultSummary,
    List<String> attachmentAssetIds,
    List<MediaAssetResponse> attachmentAssets,
    String nextReminderId,
    OffsetDateTime nextReminderAt,
    String nextReminderStatus,
    OffsetDateTime occurredAt,
    String notes,
    OffsetDateTime createdAt
) {
}
