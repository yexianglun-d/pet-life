package com.petlife.server.modules.pet.dto.response;

import java.util.List;

/**
 * 宠物主页摘要响应。
 *
 * @param pet 宠物详情
 * @param todayTodoCount 今日待办数
 * @param recentHealthRecords 最近健康记录标签
 * @param recentDailyLogs 最近日常标题
 */
public record PetSummaryResponse(
    PetDetailResponse pet,
    Integer todayTodoCount,
    List<String> recentHealthRecords,
    List<String> recentDailyLogs
) {
}
