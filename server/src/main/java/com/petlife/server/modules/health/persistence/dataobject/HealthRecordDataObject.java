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
 * @param resultSummary 结果摘要
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
    String resultSummary,
    String notes,
    LocalDateTime createdAt
) {
}
