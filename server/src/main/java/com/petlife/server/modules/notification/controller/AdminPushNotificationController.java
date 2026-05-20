package com.petlife.server.modules.notification.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.notification.dto.response.PushDeliveryRecordResponse;
import com.petlife.server.modules.notification.dto.response.PushTaskResponse;
import com.petlife.server.modules.notification.service.PushNotificationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 Push 排查控制器。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminPushNotificationController {

    private final PushNotificationApplicationService pushNotificationApplicationService;

    public AdminPushNotificationController(PushNotificationApplicationService pushNotificationApplicationService) {
        this.pushNotificationApplicationService = pushNotificationApplicationService;
    }

    @GetMapping("/push-tasks")
    public ApiResponse<List<PushTaskResponse>> listPushTasks(
        @RequestParam(value = "user_id", required = false) Long userId,
        @RequestParam(value = "notification_id", required = false) Long notificationId,
        @RequestParam(value = "task_status", required = false) String taskStatus,
        @RequestParam(value = "provider_code", required = false) String providerCode
    ) {
        return ApiResponse.success(
            pushNotificationApplicationService.listAdminPushTasks(userId, notificationId, taskStatus, providerCode)
        );
    }

    @GetMapping("/push-deliveries")
    public ApiResponse<List<PushDeliveryRecordResponse>> listDeliveryRecords(
        @RequestParam(value = "push_task_id", required = false) Long pushTaskId,
        @RequestParam(value = "user_id", required = false) Long userId,
        @RequestParam(value = "delivery_status", required = false) String deliveryStatus,
        @RequestParam(value = "provider_code", required = false) String providerCode
    ) {
        return ApiResponse.success(
            pushNotificationApplicationService.listAdminDeliveryRecords(
                pushTaskId,
                userId,
                deliveryStatus,
                providerCode
            )
        );
    }
}
