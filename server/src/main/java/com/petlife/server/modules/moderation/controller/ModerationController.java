package com.petlife.server.modules.moderation.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.dto.response.AuditLogResponse;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.moderation.dto.request.AdminReviewModerationTaskRequest;
import com.petlife.server.modules.moderation.dto.request.ProcessModerationReportRequest;
import com.petlife.server.modules.moderation.dto.response.ModerationReportResponse;
import com.petlife.server.modules.moderation.dto.response.ModerationTaskResponse;
import com.petlife.server.modules.moderation.service.ModerationApplicationService;
import com.petlife.server.modules.moderation.service.ModerationTaskApplicationService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ModerationTaskApplicationService moderationTaskApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ModerationController(
        ModerationApplicationService moderationApplicationService,
        ModerationTaskApplicationService moderationTaskApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.moderationApplicationService = moderationApplicationService;
        this.moderationTaskApplicationService = moderationTaskApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
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
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody ProcessModerationReportRequest request
    ) {
        return ApiResponse.success(
            moderationApplicationService.processReport(reportId, auditContext(operatorName, httpServletRequest), request)
        );
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ModerationTaskResponse>> listTasks(
        @RequestParam(value = "target_type", required = false) String targetType,
        @RequestParam(value = "target_id", required = false) Long targetId,
        @RequestParam(value = "content_type", required = false) String contentType,
        @RequestParam(value = "review_status", required = false) String reviewStatus,
        @RequestParam(value = "provider_code", required = false) String providerCode
    ) {
        return ApiResponse.success(
            moderationTaskApplicationService.listTasks(targetType, targetId, contentType, reviewStatus, providerCode)
        );
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<ModerationTaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(moderationTaskApplicationService.getTask(taskId));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ApiResponse<ModerationTaskResponse> reviewTask(
        @PathVariable Long taskId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminReviewModerationTaskRequest request
    ) {
        return ApiResponse.success(
            moderationTaskApplicationService.reviewTask(taskId, auditContext(operatorName, httpServletRequest), request)
        );
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLogResponse>> listAuditLogs(
        @RequestParam(value = "operator_id", required = false) String operatorId,
        @RequestParam(value = "target_type", required = false) String targetType,
        @RequestParam(value = "action", required = false) String action
    ) {
        return ApiResponse.success(auditLogApplicationService.listModerationAuditLogs(operatorId, targetType, action));
    }

    private AdminOperationContext auditContext(String operatorName, HttpServletRequest httpServletRequest) {
        return auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
    }
}
