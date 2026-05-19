package com.petlife.server.modules.auth.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 短信发送记录持久化读模型。
 */
public record SmsSendRecordDataObject(
    Long sendRecordId,
    Long verificationId,
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
