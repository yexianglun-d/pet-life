package com.petlife.server.modules.reminder.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台提醒查询数据对象。
 */
public record AdminReminderDataObject(
    Long reminderId,
    Long petId,
    String reminderType,
    String title,
    String reminderMode,
    Integer cycleValue,
    String cycleUnit,
    LocalDateTime dueAt,
    String status,
    String notes,
    LocalDateTime handledAt,
    LocalDateTime createdAt,
    String petName,
    String petType,
    Long familyId,
    String familyName,
    Long ownerUserId,
    String ownerNickname,
    String ownerMobile,
    Long handlerUserId,
    String handlerNickname,
    String handlerMobile,
    Long sourceRecordId,
    String sourceRecordType,
    String sourceRecordTitle,
    String sourceRecordStatus
) {
}
