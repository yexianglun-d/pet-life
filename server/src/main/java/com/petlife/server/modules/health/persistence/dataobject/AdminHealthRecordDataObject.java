package com.petlife.server.modules.health.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台健康记录数据对象。
 */
public record AdminHealthRecordDataObject(
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
    LocalDateTime createdAt,
    String petName,
    String petType,
    Long familyId,
    String familyName,
    Long ownerUserId,
    String ownerNickname,
    String ownerMobile,
    String operatorNickname,
    String operatorMobile
) {
}
