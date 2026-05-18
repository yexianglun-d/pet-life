package com.petlife.server.modules.reminder.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.reminder.dto.request.AdminUpdateReminderTemplateStatusRequest;
import com.petlife.server.modules.reminder.dto.request.AdminUpsertReminderTemplateRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderTemplateResponse;
import com.petlife.server.modules.reminder.service.ReminderTemplateApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台提醒模板控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/reminder-templates")
public class AdminReminderTemplateController {

    private final ReminderTemplateApplicationService reminderTemplateApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminReminderTemplateController(
        ReminderTemplateApplicationService reminderTemplateApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.reminderTemplateApplicationService = reminderTemplateApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping
    public ApiResponse<List<ReminderTemplateResponse>> listTemplates(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "reminder_type", required = false) String reminderType,
        @RequestParam(value = "default_reminder_mode", required = false) String defaultReminderMode,
        @RequestParam(value = "applicable_pet_type", required = false) String applicablePetType,
        @RequestParam(value = "enabled", required = false) Boolean enabled
    ) {
        return ApiResponse.success(
            reminderTemplateApplicationService.listAdminTemplates(
                keyword,
                reminderType,
                defaultReminderMode,
                applicablePetType,
                enabled
            )
        );
    }

    @GetMapping("/{templateId}")
    public ApiResponse<ReminderTemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(reminderTemplateApplicationService.getAdminTemplate(templateId));
    }

    @PostMapping
    public ApiResponse<ReminderTemplateResponse> createTemplate(
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertReminderTemplateRequest request
    ) {
        return ApiResponse.success(
            reminderTemplateApplicationService.createAdminTemplate(
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/{templateId}")
    public ApiResponse<ReminderTemplateResponse> updateTemplate(
        @PathVariable Long templateId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertReminderTemplateRequest request
    ) {
        return ApiResponse.success(
            reminderTemplateApplicationService.updateAdminTemplate(
                templateId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/{templateId}/status")
    public ApiResponse<ReminderTemplateResponse> updateTemplateStatus(
        @PathVariable Long templateId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateReminderTemplateStatusRequest request
    ) {
        return ApiResponse.success(
            reminderTemplateApplicationService.updateAdminTemplateStatus(
                templateId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    private AdminOperationContext auditContext(
        String operatorName,
        HttpServletRequest httpServletRequest
    ) {
        return auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
    }
}
