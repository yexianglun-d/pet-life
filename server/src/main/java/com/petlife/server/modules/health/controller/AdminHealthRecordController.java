package com.petlife.server.modules.health.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.health.dto.response.AdminHealthRecordResponse;
import com.petlife.server.modules.health.service.HealthApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台健康记录查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/health-records")
public class AdminHealthRecordController {

    private final HealthApplicationService healthApplicationService;

    public AdminHealthRecordController(HealthApplicationService healthApplicationService) {
        this.healthApplicationService = healthApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminHealthRecordResponse>> listHealthRecords(
        @RequestParam(value = "record_type", required = false) String recordType,
        @RequestParam(value = "pet_id", required = false) Long petId,
        @RequestParam(value = "operator_user_id", required = false) Long operatorUserId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(
            healthApplicationService.listAdminHealthRecords(recordType, petId, operatorUserId, keyword)
        );
    }

    @GetMapping("/{healthRecordId}")
    public ApiResponse<AdminHealthRecordResponse> getHealthRecord(@PathVariable Long healthRecordId) {
        return ApiResponse.success(healthApplicationService.getAdminHealthRecord(healthRecordId));
    }
}
