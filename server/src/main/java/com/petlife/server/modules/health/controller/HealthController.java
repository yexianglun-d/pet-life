package com.petlife.server.modules.health.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.health.dto.request.CreateHealthRecordRequest;
import com.petlife.server.modules.health.dto.request.UpdateHealthRecordRequest;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.health.service.HealthApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康记录控制器。
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/health-records")
public class HealthController {

    private final HealthApplicationService healthApplicationService;

    public HealthController(HealthApplicationService healthApplicationService) {
        this.healthApplicationService = healthApplicationService;
    }

    @GetMapping
    public ApiResponse<List<HealthRecordResponse>> listHealthRecords(@PathVariable Long petId) {
        return ApiResponse.success(healthApplicationService.listHealthRecords(petId));
    }

    @GetMapping("/{healthRecordId}")
    public ApiResponse<HealthRecordResponse> getHealthRecord(
        @PathVariable Long petId,
        @PathVariable Long healthRecordId
    ) {
        return ApiResponse.success(healthApplicationService.getHealthRecord(petId, healthRecordId));
    }

    @PostMapping
    public ApiResponse<HealthRecordResponse> createHealthRecord(
        @PathVariable Long petId,
        @Valid @RequestBody CreateHealthRecordRequest request
    ) {
        return ApiResponse.success(healthApplicationService.createHealthRecord(petId, request));
    }

    @PatchMapping("/{healthRecordId}")
    public ApiResponse<HealthRecordResponse> updateHealthRecord(
        @PathVariable Long petId,
        @PathVariable Long healthRecordId,
        @Valid @RequestBody UpdateHealthRecordRequest request
    ) {
        return ApiResponse.success(healthApplicationService.updateHealthRecord(petId, healthRecordId, request));
    }

    @DeleteMapping("/{healthRecordId}")
    public ApiResponse<Void> deleteHealthRecord(
        @PathVariable Long petId,
        @PathVariable Long healthRecordId
    ) {
        healthApplicationService.deleteHealthRecord(petId, healthRecordId);
        return ApiResponse.success(null);
    }
}
