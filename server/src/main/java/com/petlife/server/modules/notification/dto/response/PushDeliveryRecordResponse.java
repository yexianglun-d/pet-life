package com.petlife.server.modules.notification.dto.response;

import java.time.OffsetDateTime;

/**
 * Push 投递记录响应。
 */
public record PushDeliveryRecordResponse(
    String deliveryRecordId,
    String pushTaskId,
    String deviceTokenId,
    String userId,
    String providerCode,
    String deliveryStatus,
    String failureReason,
    OffsetDateTime attemptedAt,
    OffsetDateTime createdAt
) {
}
