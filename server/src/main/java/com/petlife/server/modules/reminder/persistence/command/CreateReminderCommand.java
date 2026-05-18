package com.petlife.server.modules.reminder.persistence.command;

import java.time.LocalDateTime;

/**
 * 创建提醒命令。
 */
public class CreateReminderCommand {

    private Long id;
    private Long petId;
    private String reminderType;
    private String title;
    private String reminderMode;
    private Integer cycleValue;
    private String cycleUnit;
    private LocalDateTime dueAt;
    private Long sourceRecordId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReminderMode() {
        return reminderMode;
    }

    public void setReminderMode(String reminderMode) {
        this.reminderMode = reminderMode;
    }

    public Integer getCycleValue() {
        return cycleValue;
    }

    public void setCycleValue(Integer cycleValue) {
        this.cycleValue = cycleValue;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public void setCycleUnit(String cycleUnit) {
        this.cycleUnit = cycleUnit;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(Long sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }
}
