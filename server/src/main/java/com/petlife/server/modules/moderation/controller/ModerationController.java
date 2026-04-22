package com.petlife.server.modules.moderation.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.moderation.dto.request.ProcessModerationReportRequest;
import com.petlife.server.modules.moderation.dto.response.ModerationReportResponse;
import com.petlife.server.modules.moderation.service.ModerationApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审核中心控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/moderation")
public class ModerationController {

    private final ModerationApplicationService moderationApplicationService;

    public ModerationController(ModerationApplicationService moderationApplicationService) {
        this.moderationApplicationService = moderationApplicationService;
    }

    @GetMapping("/reports")
    public ApiResponse<List<ModerationReportResponse>> listReports(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(moderationApplicationService.listReports(status));
    }

    @PatchMapping("/reports/{reportId}")
    public ApiResponse<ModerationReportResponse> processReport(
        @PathVariable Long reportId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        @Valid @RequestBody ProcessModerationReportRequest request
    ) {
        return ApiResponse.success(
            moderationApplicationService.processReport(reportId, operatorName, request)
        );
    }
}
