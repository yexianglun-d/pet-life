package com.petlife.server.modules.auth.dto.response;

import java.time.LocalDateTime;

/**
 * 后台短信发送记录响应。
 *
 * @param sendRecordId 发送记录 ID
 * @param verificationId 验证码记录 ID
 * @param mobile 手机号
 * @param scene 业务场景
 * @param providerCode 供应商编码
 * @param sendStatus 发送状态
 * @param failureReason 失败原因
 * @param requestIp 请求 IP
 * @param userAgent 客户端标识
 * @param createdAt 创建时间
 */
public record SmsSendRecordResponse(
    String sendRecordId,
    String verificationId,
    String mobile,
    String scene,
    String providerCode,
    String sendStatus,
    String failureReason,
    String requestIp,
    String userAgent,
    LocalDateTime createdAt
) {
}
