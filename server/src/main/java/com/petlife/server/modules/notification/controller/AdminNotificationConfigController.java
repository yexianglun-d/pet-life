package com.petlife.server.modules.notification.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.dto.response.AuditLogResponse;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.notification.dto.request.AdminUpdateMessageTemplateStatusRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpdateNotificationChannelStatusRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpsertMessageTemplateRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpsertNotificationChannelRequest;
import com.petlife.server.modules.notification.dto.response.MessageTemplateResponse;
import com.petlife.server.modules.notification.dto.response.NotificationChannelConfigResponse;
import com.petlife.server.modules.notification.service.NotificationConfigApplicationService;
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
 * 后台通知与消息配置控制器。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminNotificationConfigController {

    private final NotificationConfigApplicationService notificationConfigApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminNotificationConfigController(
        NotificationConfigApplicationService notificationConfigApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.notificationConfigApplicationService = notificationConfigApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping("/message-templates")
    public ApiResponse<List<MessageTemplateResponse>> listMessageTemplates(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "template_code", required = false) String templateCode,
        @RequestParam(value = "channel_type", required = false) String channelType,
        @RequestParam(value = "enabled", required = false) Boolean enabled
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.listMessageTemplates(keyword, templateCode, channelType, enabled)
        );
    }

    @GetMapping("/message-templates/{templateId}")
    public ApiResponse<MessageTemplateResponse> getMessageTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(notificationConfigApplicationService.getMessageTemplate(templateId));
    }

    @PostMapping("/message-templates")
    public ApiResponse<MessageTemplateResponse> createMessageTemplate(
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertMessageTemplateRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.createMessageTemplate(
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/message-templates/{templateId}")
    public ApiResponse<MessageTemplateResponse> updateMessageTemplate(
        @PathVariable Long templateId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertMessageTemplateRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.updateMessageTemplate(
                templateId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/message-templates/{templateId}/status")
    public ApiResponse<MessageTemplateResponse> updateMessageTemplateStatus(
        @PathVariable Long templateId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateMessageTemplateStatusRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.updateMessageTemplateStatus(
                templateId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @GetMapping("/notification-channels")
    public ApiResponse<List<NotificationChannelConfigResponse>> listNotificationChannels(
        @RequestParam(value = "channel_type", required = false) String channelType,
        @RequestParam(value = "provider_code", required = false) String providerCode,
        @RequestParam(value = "enabled", required = false) Boolean enabled,
        @RequestParam(value = "config_status", required = false) String configStatus
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.listNotificationChannels(
                channelType,
                providerCode,
                enabled,
                configStatus
            )
        );
    }

    @GetMapping("/notification-channels/{channelConfigId}")
    public ApiResponse<NotificationChannelConfigResponse> getNotificationChannel(@PathVariable Long channelConfigId) {
        return ApiResponse.success(notificationConfigApplicationService.getNotificationChannel(channelConfigId));
    }

    @PostMapping("/notification-channels")
    public ApiResponse<NotificationChannelConfigResponse> createNotificationChannel(
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertNotificationChannelRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.createNotificationChannel(
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/notification-channels/{channelConfigId}")
    public ApiResponse<NotificationChannelConfigResponse> updateNotificationChannel(
        @PathVariable Long channelConfigId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertNotificationChannelRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.updateNotificationChannel(
                channelConfigId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/notification-channels/{channelConfigId}/status")
    public ApiResponse<NotificationChannelConfigResponse> updateNotificationChannelStatus(
        @PathVariable Long channelConfigId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateNotificationChannelStatusRequest request
    ) {
        return ApiResponse.success(
            notificationConfigApplicationService.updateNotificationChannelStatus(
                channelConfigId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @GetMapping("/notification/audit-logs")
    public ApiResponse<List<AuditLogResponse>> listAuditLogs(
        @RequestParam(value = "operator_id", required = false) String operatorId,
        @RequestParam(value = "target_type", required = false) String targetType,
        @RequestParam(value = "action", required = false) String action
    ) {
        return ApiResponse.success(auditLogApplicationService.listNotificationAuditLogs(operatorId, targetType, action));
    }

    private AdminOperationContext auditContext(
        String operatorName,
        HttpServletRequest httpServletRequest
    ) {
        return auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
    }
}
