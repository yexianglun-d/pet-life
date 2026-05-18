package com.petlife.server.modules.notification.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.notification.dto.request.MarkNotificationsReadRequest;
import com.petlife.server.modules.notification.dto.response.NotificationListResponse;
import com.petlife.server.modules.notification.dto.response.NotificationResponse;
import com.petlife.server.modules.notification.service.NotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知控制器。
 */
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping("/notifications")
    public ApiResponse<NotificationListResponse> listNotifications(
        @RequestParam(value = "notify_type", required = false) String notifyType,
        @RequestParam(value = "read_status", required = false) String readStatus
    ) {
        return ApiResponse.success(notificationApplicationService.listNotifications(notifyType, readStatus));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ApiResponse<NotificationResponse> markNotificationRead(@PathVariable Long notificationId) {
        return ApiResponse.success(notificationApplicationService.markNotificationRead(notificationId));
    }

    @PatchMapping("/notifications/read")
    public ApiResponse<NotificationListResponse> markNotificationsRead(
        @Valid @RequestBody(required = false) MarkNotificationsReadRequest request
    ) {
        return ApiResponse.success(notificationApplicationService.markNotificationsRead(request));
    }
}
