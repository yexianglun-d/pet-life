package com.petlife.server.modules.moderation.domain.entity;

import java.time.LocalDateTime;

/**
 * 内容审核任务实体。
 *
 * <p>审核任务是公开内容进入社区曝光前的状态机载体，供应商回调和人工处理都只更新该实体，
 * 再由应用服务把最终结论同步到目标内容。</p>
 */
public final class ModerationTaskEntity {

    private final Long taskId;
    private final String targetType;
    private final Long targetId;
    private final String contentType;
    private final String contentSnapshot;
    private final String providerCode;
    private final String reviewStatus;
    private final String reviewResult;
    private final String riskLabels;
    private final String failureReason;
    private final String callbackPayload;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ModerationTaskEntity(
        Long taskId,
        String targetType,
        Long targetId,
        String contentType,
        String contentSnapshot,
        String providerCode,
        String reviewStatus,
        String reviewResult,
        String riskLabels,
        String failureReason,
        String callbackPayload,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.taskId = taskId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.contentType = contentType;
        this.contentSnapshot = contentSnapshot;
        this.providerCode = providerCode;
        this.reviewStatus = reviewStatus;
        this.reviewResult = reviewResult;
        this.riskLabels = riskLabels;
        this.failureReason = failureReason;
        this.callbackPayload = callbackPayload;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public String getRiskLabels() {
        return riskLabels;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCallbackPayload() {
        return callbackPayload;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
