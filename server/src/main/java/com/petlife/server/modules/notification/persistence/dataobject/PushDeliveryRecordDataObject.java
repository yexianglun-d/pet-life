package com.petlife.server.modules.notification.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * Push 投递记录数据对象。
 */
public record PushDeliveryRecordDataObject(
    Long deliveryRecordId,
    Long pushTaskId,
    Long deviceTokenId,
    Long userId,
    String providerCode,
    String deliveryStatus,
    String failureReason,
    LocalDateTime attemptedAt,
    LocalDateTime createdAt
) {
}
