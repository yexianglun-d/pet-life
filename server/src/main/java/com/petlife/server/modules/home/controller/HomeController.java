package com.petlife.server.modules.home.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.home.dto.response.HomePetReportResponse;
import com.petlife.server.modules.home.service.HomeApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器。
 */
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeApplicationService homeApplicationService;

    public HomeController(HomeApplicationService homeApplicationService) {
        this.homeApplicationService = homeApplicationService;
    }

    @GetMapping("/reports/weekly")
    public ApiResponse<HomePetReportResponse> getWeeklyReport() {
        return ApiResponse.success(homeApplicationService.getWeeklyReport());
    }

    @GetMapping("/reports/monthly")
    public ApiResponse<HomePetReportResponse> getMonthlyReport() {
        return ApiResponse.success(homeApplicationService.getMonthlyReport());
    }
}
