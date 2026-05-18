package com.petlife.server.modules.health.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康记录实体。
 *
 * <p>该实体用于描述宠物在健康维度发生的单条业务事件，
 * 包括疫苗、驱虫、体检、问诊等统一记录。</p>
 */
public final class HealthRecordEntity {

    private final Long healthRecordId;
    private final Long petId;
    private final Long operatorUserId;
    private final String recordType;
    private final String title;
    private final LocalDateTime occurredAt;
    private final String hospitalName;
    private final String doctorName;
    private final String severityLevel;
    private final String resultSummary;
    private final List<String> attachmentAssetIds;
    private final Long nextReminderId;
    private final LocalDateTime nextReminderAt;
    private final String nextReminderStatus;
    private final String notes;
    private final LocalDateTime createdAt;

    public HealthRecordEntity(
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
        List<String> attachmentAssetIds,
        Long nextReminderId,
        LocalDateTime nextReminderAt,
        String nextReminderStatus,
        String notes,
        LocalDateTime createdAt
    ) {
        this.healthRecordId = healthRecordId;
        this.petId = petId;
        this.operatorUserId = operatorUserId;
        this.recordType = recordType;
        this.title = title;
        this.occurredAt = occurredAt;
        this.hospitalName = hospitalName;
        this.doctorName = doctorName;
        this.severityLevel = severityLevel;
        this.resultSummary = resultSummary;
        this.attachmentAssetIds = attachmentAssetIds;
        this.nextReminderId = nextReminderId;
        this.nextReminderAt = nextReminderAt;
        this.nextReminderStatus = nextReminderStatus;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getHealthRecordId() {
        return healthRecordId;
    }

    public Long getPetId() {
        return petId;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public List<String> getAttachmentAssetIds() {
        return attachmentAssetIds;
    }

    public Long getNextReminderId() {
        return nextReminderId;
    }

    public LocalDateTime getNextReminderAt() {
        return nextReminderAt;
    }

    public String getNextReminderStatus() {
        return nextReminderStatus;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
