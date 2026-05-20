package com.petlife.server.modules.moderation.service.provider;

/**
 * 内容审核提交结果。
 */
public record ModerationSubmissionResult(
    String reviewStatus,
    String reviewResult,
    String riskLabels,
    String failureReason
) {
}
