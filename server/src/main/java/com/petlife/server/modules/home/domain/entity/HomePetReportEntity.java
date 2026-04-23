package com.petlife.server.modules.home.domain.entity;

import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页宠物周期报告实体。
 *
 * <p>周报和月报不是新的事实源，而是对提醒、健康记录和萌宠日常的周期聚合读模型。
 * 这里统一沉淀周期窗口、照护统计和最近事件，避免移动端自行拼装出多份不一致报表。</p>
 */
public final class HomePetReportEntity {

    private final String reportType;
    private final PetProfileEntity pet;
    private final LocalDateTime windowStart;
    private final LocalDateTime windowEnd;
    private final int pendingReminderCount;
    private final int completedReminderCount;
    private final int skippedReminderCount;
    private final int healthRecordCount;
    private final int dailyLogCount;
    private final int communitySyncCount;
    private final int feedCount;
    private final int waterCount;
    private final int toiletCount;
    private final int weightRecordCount;
    private final int medicationRecordCount;
    private final List<String> highlights;
    private final List<ReminderEntity> recentReminders;
    private final List<HealthRecordEntity> recentHealthRecords;
    private final List<DailyLogEntity> recentDailyLogs;

    public HomePetReportEntity(
        String reportType,
        PetProfileEntity pet,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        int pendingReminderCount,
        int completedReminderCount,
        int skippedReminderCount,
        int healthRecordCount,
        int dailyLogCount,
        int communitySyncCount,
        int feedCount,
        int waterCount,
        int toiletCount,
        int weightRecordCount,
        int medicationRecordCount,
        List<String> highlights,
        List<ReminderEntity> recentReminders,
        List<HealthRecordEntity> recentHealthRecords,
        List<DailyLogEntity> recentDailyLogs
    ) {
        this.reportType = reportType;
        this.pet = pet;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.pendingReminderCount = pendingReminderCount;
        this.completedReminderCount = completedReminderCount;
        this.skippedReminderCount = skippedReminderCount;
        this.healthRecordCount = healthRecordCount;
        this.dailyLogCount = dailyLogCount;
        this.communitySyncCount = communitySyncCount;
        this.feedCount = feedCount;
        this.waterCount = waterCount;
        this.toiletCount = toiletCount;
        this.weightRecordCount = weightRecordCount;
        this.medicationRecordCount = medicationRecordCount;
        this.highlights = highlights == null ? List.of() : List.copyOf(highlights);
        this.recentReminders = recentReminders == null ? List.of() : List.copyOf(recentReminders);
        this.recentHealthRecords = recentHealthRecords == null ? List.of() : List.copyOf(recentHealthRecords);
        this.recentDailyLogs = recentDailyLogs == null ? List.of() : List.copyOf(recentDailyLogs);
    }

    public String getReportType() {
        return reportType;
    }

    public PetProfileEntity getPet() {
        return pet;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public int getPendingReminderCount() {
        return pendingReminderCount;
    }

    public int getCompletedReminderCount() {
        return completedReminderCount;
    }

    public int getSkippedReminderCount() {
        return skippedReminderCount;
    }

    public int getHealthRecordCount() {
        return healthRecordCount;
    }

    public int getDailyLogCount() {
        return dailyLogCount;
    }

    public int getCommunitySyncCount() {
        return communitySyncCount;
    }

    public int getFeedCount() {
        return feedCount;
    }

    public int getWaterCount() {
        return waterCount;
    }

    public int getToiletCount() {
        return toiletCount;
    }

    public int getWeightRecordCount() {
        return weightRecordCount;
    }

    public int getMedicationRecordCount() {
        return medicationRecordCount;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public List<ReminderEntity> getRecentReminders() {
        return recentReminders;
    }

    public List<HealthRecordEntity> getRecentHealthRecords() {
        return recentHealthRecords;
    }

    public List<DailyLogEntity> getRecentDailyLogs() {
        return recentDailyLogs;
    }
}
