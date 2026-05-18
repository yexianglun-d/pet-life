package com.petlife.server.modules.reminder.domain.entity;

/**
 * 后台提醒来源记录实体。
 */
public final class AdminReminderSourceEntity {

    private final Long sourceRecordId;
    private final String recordType;
    private final String title;
    private final String status;

    public AdminReminderSourceEntity(
        Long sourceRecordId,
        String recordType,
        String title,
        String status
    ) {
        this.sourceRecordId = sourceRecordId;
        this.recordType = recordType;
        this.title = title;
        this.status = status;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }
}
