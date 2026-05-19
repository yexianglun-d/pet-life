package com.petlife.server.modules.auth.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.auth.dto.response.SmsSendRecordResponse;
import com.petlife.server.modules.auth.dto.response.SmsVerificationRecordResponse;
import com.petlife.server.modules.auth.service.SmsVerificationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台短信验证码排查控制器。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminSmsVerificationController {

    private final SmsVerificationApplicationService smsVerificationApplicationService;

    public AdminSmsVerificationController(SmsVerificationApplicationService smsVerificationApplicationService) {
        this.smsVerificationApplicationService = smsVerificationApplicationService;
    }

    @GetMapping("/sms-verifications")
    public ApiResponse<List<SmsVerificationRecordResponse>> listVerificationRecords(
        @RequestParam(value = "mobile", required = false) String mobile,
        @RequestParam(value = "scene", required = false) String scene,
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(smsVerificationApplicationService.listVerificationRecords(mobile, scene, status));
    }

    @GetMapping("/sms-send-records")
    public ApiResponse<List<SmsSendRecordResponse>> listSendRecords(
        @RequestParam(value = "mobile", required = false) String mobile,
        @RequestParam(value = "scene", required = false) String scene,
        @RequestParam(value = "provider_code", required = false) String providerCode,
        @RequestParam(value = "send_status", required = false) String sendStatus
    ) {
        return ApiResponse.success(
            smsVerificationApplicationService.listSendRecords(mobile, scene, providerCode, sendStatus)
        );
    }
}
