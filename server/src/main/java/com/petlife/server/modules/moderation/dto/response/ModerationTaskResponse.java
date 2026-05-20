package com.petlife.server.modules.moderation.dto.response;

import java.time.OffsetDateTime;

/**
 * 内容审核任务响应。
 *
 * @param taskId 审核任务 ID
 * @param targetType 目标内容类型
 * @param targetId 目标内容 ID
 * @param contentType 内容形态
 * @param contentSnapshot 审核快照 JSON
 * @param providerCode 审核供应商编码
 * @param reviewStatus 审核状态
 * @param reviewResult 审核结果 JSON
 * @param riskLabels 风险标签 JSON
 * @param failureReason 失败原因
 * @param callbackPayload 回调原始载荷 JSON
 * @param reviewedAt 审核完成时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ModerationTaskResponse(
    String taskId,
    String targetType,
    String targetId,
    String contentType,
    String contentSnapshot,
    String providerCode,
    String reviewStatus,
    String reviewResult,
    String riskLabels,
    String failureReason,
    String callbackPayload,
    OffsetDateTime reviewedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
