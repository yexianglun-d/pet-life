package com.petlife.server.modules.notification.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.notification.dto.request.RegisterPushDeviceTokenRequest;
import com.petlife.server.modules.notification.dto.response.PushDeviceTokenResponse;
import com.petlife.server.modules.notification.service.PushNotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App Push 设备 Token 控制器。
 */
@RestController
@RequestMapping("/api/v1/push/device-tokens")
public class PushDeviceTokenController {

    private final PushNotificationApplicationService pushNotificationApplicationService;

    public PushDeviceTokenController(PushNotificationApplicationService pushNotificationApplicationService) {
        this.pushNotificationApplicationService = pushNotificationApplicationService;
    }

    @PostMapping
    public ApiResponse<PushDeviceTokenResponse> registerDeviceToken(
        @Valid @RequestBody RegisterPushDeviceTokenRequest request
    ) {
        return ApiResponse.success(pushNotificationApplicationService.registerDeviceToken(request));
    }

    @DeleteMapping("/{deviceTokenId}")
    public ApiResponse<PushDeviceTokenResponse> unregisterDeviceToken(@PathVariable Long deviceTokenId) {
        return ApiResponse.success(pushNotificationApplicationService.unregisterDeviceToken(deviceTokenId));
    }
}
