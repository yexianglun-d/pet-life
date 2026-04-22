package com.petlife.server.modules.reminder.domain.entity;

import java.time.LocalDateTime;

/**
 * 提醒实体。
 *
 * <p>该实体统一表达提醒待办的状态与时间信息，
 * 用于首页、宠物页和后续通知中心共享。</p>
 */
public final class ReminderEntity {

    private final Long reminderId;
    private final Long petId;
    private final String reminderType;
    private final String title;
    private final String reminderMode;
    private final Integer cycleValue;
    private final String cycleUnit;
    private final LocalDateTime dueAt;
    private final String status;
    private final String notes;
    private final LocalDateTime handledAt;
    private final LocalDateTime createdAt;

    public ReminderEntity(
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
        LocalDateTime createdAt
    ) {
        this.reminderId = reminderId;
        this.petId = petId;
        this.reminderType = reminderType;
        this.title = title;
        this.reminderMode = reminderMode;
        this.cycleValue = cycleValue;
        this.cycleUnit = cycleUnit;
        this.dueAt = dueAt;
        this.status = status;
        this.notes = notes;
        this.handledAt = handledAt;
        this.createdAt = createdAt;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public Long getPetId() {
        return petId;
    }

    public String getReminderType() {
        return reminderType;
    }

    public String getTitle() {
        return title;
    }

    public String getReminderMode() {
        return reminderMode;
    }

    public Integer getCycleValue() {
        return cycleValue;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
