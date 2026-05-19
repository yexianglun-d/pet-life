package com.petlife.server.modules.auth.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 短信验证码持久化读模型。
 */
public record SmsVerificationCodeDataObject(
    Long verificationId,
    String mobile,
    String scene,
    String codeHash,
    String salt,
    LocalDateTime expiresAt,
    LocalDateTime verifiedAt,
    Integer attemptCount,
    Integer maxAttemptCount,
    String status,
    String requestIp,
    String userAgent,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
