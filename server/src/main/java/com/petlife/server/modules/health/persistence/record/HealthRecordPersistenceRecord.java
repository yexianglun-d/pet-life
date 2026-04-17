package com.petlife.server.modules.health.persistence.record;

import java.time.LocalDateTime;

/**
 * 健康记录持久化记录。
 *
 * @param healthRecordId 健康记录 ID
 * @param petId 宠物 ID
 * @param operatorUserId 操作人用户 ID
 * @param recordType 记录类型
 * @param title 记录标题
 * @param occurredAt 发生时间
 * @param notes 备注
 * @param createdAt 创建时间
 */
public record HealthRecordPersistenceRecord(
    Long healthRecordId,
    Long petId,
    Long operatorUserId,
    String recordType,
    String title,
    LocalDateTime occurredAt,
    String notes,
    LocalDateTime createdAt
) {
}
