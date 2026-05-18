package com.petlife.server.modules.service.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 服务商评价持久化读模型。
 */
public record ProviderReviewDataObject(
    Long reviewId,
    Long providerId,
    String providerName,
    String providerType,
    Long appointmentId,
    Long userId,
    String reviewerNickname,
    Long petId,
    String petName,
    Integer rating,
    String content,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
