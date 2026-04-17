package com.petlife.server.modules.dailylog.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.dailylog.dto.request.CreateDailyLogRequest;
import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.dailylog.service.DailyLogApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 萌宠日常控制器。
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/daily-logs")
public class DailyLogController {

    private final DailyLogApplicationService dailyLogApplicationService;

    public DailyLogController(DailyLogApplicationService dailyLogApplicationService) {
        this.dailyLogApplicationService = dailyLogApplicationService;
    }

    @GetMapping
    public ApiResponse<List<DailyLogResponse>> listDailyLogs(@PathVariable Long petId) {
        return ApiResponse.success(dailyLogApplicationService.listDailyLogs(petId));
    }

    @PostMapping
    public ApiResponse<DailyLogResponse> createDailyLog(
        @PathVariable Long petId,
        @Valid @RequestBody CreateDailyLogRequest request
    ) {
        return ApiResponse.success(dailyLogApplicationService.createDailyLog(petId, request));
    }
}
