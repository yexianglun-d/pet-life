package com.petlife.server.modules.health.domain.entity;

import java.time.LocalDateTime;

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
    private final String resultSummary;
    private final String notes;
    private final LocalDateTime createdAt;

    public HealthRecordEntity(
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
        this.healthRecordId = healthRecordId;
        this.petId = petId;
        this.operatorUserId = operatorUserId;
        this.recordType = recordType;
        this.title = title;
        this.occurredAt = occurredAt;
        this.resultSummary = resultSummary;
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

    public String getResultSummary() {
        return resultSummary;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
