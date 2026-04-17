package com.petlife.server.bootstrap.devsupport.model;

import java.time.OffsetDateTime;

/**
 * 开发期健康记录模型。
 *
 * @param healthRecordId 健康记录 ID
 * @param petId 宠物 ID
 * @param recordType 记录类型
 * @param title 记录标题
 * @param value 数值
 * @param unit 单位
 * @param occurredAt 发生时间
 * @param notes 备注
 * @param createdAt 创建时间
 */
public record DevHealthRecord(
    Long healthRecordId,
    Long petId,
    String recordType,
    String title,
    String value,
    String unit,
    OffsetDateTime occurredAt,
    String notes,
    OffsetDateTime createdAt
) {
}
