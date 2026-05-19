package com.petlife.server.modules.auth.service.sms;

/**
 * 短信发送结果。
 *
 * @param sendStatus 发送状态
 * @param failureReason 失败原因
 */
public record SmsSendResult(
    String sendStatus,
    String failureReason
) {
}
