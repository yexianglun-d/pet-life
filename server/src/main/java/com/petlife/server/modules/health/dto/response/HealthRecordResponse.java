package com.petlife.server.modules.health.dto.response;

import java.time.OffsetDateTime;

/**
 * 健康记录响应。
 *
 * @param healthRecordId 健康记录 ID
 * @param petId 宠物 ID
 * @param recordType 记录类型
 * @param title 标题
 * @param value 数值
 * @param unit 单位
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
    OffsetDateTime occurredAt,
    String notes,
    OffsetDateTime createdAt
) {
}
