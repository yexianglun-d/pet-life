package com.petlife.server.modules.dailylog.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.dailylog.dto.response.AdminDailyLogResponse;
import com.petlife.server.modules.dailylog.service.DailyLogApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台萌宠日常查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/daily-logs")
public class AdminDailyLogController {

    private final DailyLogApplicationService dailyLogApplicationService;

    public AdminDailyLogController(DailyLogApplicationService dailyLogApplicationService) {
        this.dailyLogApplicationService = dailyLogApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminDailyLogResponse>> listDailyLogs(
        @RequestParam(value = "visibility", required = false) String visibility,
        @RequestParam(value = "sync_to_community", required = false) Boolean syncToCommunity,
        @RequestParam(value = "pet_id", required = false) Long petId,
        @RequestParam(value = "author_user_id", required = false) Long authorUserId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(
            dailyLogApplicationService.listAdminDailyLogs(
                visibility,
                syncToCommunity,
                petId,
                authorUserId,
                keyword
            )
        );
    }

    @GetMapping("/{dailyLogId}")
    public ApiResponse<AdminDailyLogResponse> getDailyLog(@PathVariable Long dailyLogId) {
        return ApiResponse.success(dailyLogApplicationService.getAdminDailyLog(dailyLogId));
    }
}
