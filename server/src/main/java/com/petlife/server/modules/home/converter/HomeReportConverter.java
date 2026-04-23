package com.petlife.server.modules.home.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.dailylog.converter.DailyLogEntityConverter;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.home.domain.entity.HomePetReportEntity;
import com.petlife.server.modules.home.dto.response.HomePetReportResponse;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.reminder.converter.ReminderEntityConverter;
import org.springframework.stereotype.Component;

/**
 * 首页报表转换器。
 */
@Component
public class HomeReportConverter {

    private final PetEntityConverter petEntityConverter;
    private final ReminderEntityConverter reminderEntityConverter;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final DailyLogEntityConverter dailyLogEntityConverter;

    public HomeReportConverter(
        PetEntityConverter petEntityConverter,
        ReminderEntityConverter reminderEntityConverter,
        HealthRecordEntityConverter healthRecordEntityConverter,
        DailyLogEntityConverter dailyLogEntityConverter
    ) {
        this.petEntityConverter = petEntityConverter;
        this.reminderEntityConverter = reminderEntityConverter;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
    }

    public HomePetReportResponse toResponse(HomePetReportEntity homePetReport) {
        return new HomePetReportResponse(
            homePetReport.getReportType(),
            petEntityConverter.toPetDetailResponse(homePetReport.getPet()),
            DateTimeConverters.toOffsetDateTime(homePetReport.getWindowStart()),
            DateTimeConverters.toOffsetDateTime(homePetReport.getWindowEnd()),
            homePetReport.getPendingReminderCount(),
            homePetReport.getCompletedReminderCount(),
            homePetReport.getSkippedReminderCount(),
            homePetReport.getHealthRecordCount(),
            homePetReport.getDailyLogCount(),
            homePetReport.getCommunitySyncCount(),
            homePetReport.getFeedCount(),
            homePetReport.getWaterCount(),
            homePetReport.getToiletCount(),
            homePetReport.getWeightRecordCount(),
            homePetReport.getMedicationRecordCount(),
            homePetReport.getHighlights(),
            homePetReport.getRecentReminders().stream()
                .map(reminderEntityConverter::toResponse)
                .toList(),
            homePetReport.getRecentHealthRecords().stream()
                .map(healthRecordEntityConverter::toResponse)
                .toList(),
            homePetReport.getRecentDailyLogs().stream()
                .map(dailyLogEntityConverter::toResponse)
                .toList()
        );
    }
}
