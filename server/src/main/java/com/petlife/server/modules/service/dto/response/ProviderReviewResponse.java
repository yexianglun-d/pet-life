package com.petlife.server.modules.service.dto.response;

import java.time.OffsetDateTime;

/**
 * 服务商评价响应。
 *
 * @param reviewId 评价 ID
 * @param providerId 服务商 ID
 * @param providerName 服务商名称
 * @param providerType 服务商类型
 * @param appointmentId 预约 ID
 * @param userId 评价用户 ID
 * @param reviewerNickname 评价用户昵称
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param rating 评分
 * @param content 评价内容
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ProviderReviewResponse(
    String reviewId,
    String providerId,
    String providerName,
    String providerType,
    String appointmentId,
    String userId,
    String reviewerNickname,
    String petId,
    String petName,
    Integer rating,
    String content,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
