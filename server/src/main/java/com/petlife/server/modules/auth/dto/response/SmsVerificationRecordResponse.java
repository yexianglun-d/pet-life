package com.petlife.server.modules.auth.dto.response;

import java.time.LocalDateTime;

/**
 * 后台短信验证码校验记录响应。
 *
 * @param verificationId 验证码记录 ID
 * @param mobile 手机号
 * @param scene 业务场景
 * @param expiresAt 过期时间
 * @param verifiedAt 验证通过时间
 * @param attemptCount 已尝试次数
 * @param maxAttemptCount 最大尝试次数
 * @param status 验证码状态
 * @param requestIp 请求 IP
 * @param userAgent 客户端标识
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record SmsVerificationRecordResponse(
    String verificationId,
    String mobile,
    String scene,
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
