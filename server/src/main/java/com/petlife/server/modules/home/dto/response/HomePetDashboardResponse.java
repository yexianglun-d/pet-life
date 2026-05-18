package com.petlife.server.modules.home.dto.response;

import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import java.util.List;

/**
 * 首页当前宠物面板响应。
 *
 * @param pet 当前宠物详情
 * @param todayTodoCount 待处理提醒数
 * @param reminders 提醒列表
 * @param healthRecords 最近健康记录
 * @param dailyLogs 最近日常记录
 */
public record HomePetDashboardResponse(
    PetDetailResponse pet,
    Integer todayTodoCount,
    List<ReminderResponse> reminders,
    List<HealthRecordResponse> healthRecords,
    List<DailyLogResponse> dailyLogs
) {
}
