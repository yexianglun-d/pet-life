package com.petlife.server.modules.home.dto.response;

import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 首页宠物周期报告响应。
 *
 * @param reportType 报告类型：weekly/monthly
 * @param pet 当前宠物
 * @param windowStart 统计窗口开始时间
 * @param windowEnd 统计窗口结束时间
 * @param pendingReminderCount 待处理提醒数
 * @param completedReminderCount 已完成提醒数
 * @param skippedReminderCount 已跳过提醒数
 * @param healthRecordCount 健康记录数
 * @param dailyLogCount 日常记录数
 * @param communitySyncCount 同步社区数
 * @param feedCount 喂食快捷记录数
 * @param waterCount 饮水快捷记录数
 * @param toiletCount 排便快捷记录数
 * @param weightRecordCount 体重记录数
 * @param medicationRecordCount 用药记录数
 * @param highlights 报告亮点
 * @param recentReminders 最近提醒
 * @param recentHealthRecords 最近健康记录
 * @param recentDailyLogs 最近萌宠日常
 */
public record HomePetReportResponse(
    String reportType,
    PetDetailResponse pet,
    OffsetDateTime windowStart,
    OffsetDateTime windowEnd,
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
    List<ReminderResponse> recentReminders,
    List<HealthRecordResponse> recentHealthRecords,
    List<DailyLogResponse> recentDailyLogs
) {
}
