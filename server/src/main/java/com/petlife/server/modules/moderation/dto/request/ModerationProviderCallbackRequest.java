package com.petlife.server.modules.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 审核供应商回调请求。
 *
 * @param taskId 审核任务 ID
 * @param reviewStatus 审核状态：approved/rejected/failed
 * @param reviewResult 审核结果详情
 * @param riskLabels 风险标签
 * @param failureReason 失败原因
 * @param callbackPayload 回调原始载荷
 */
public record ModerationProviderCallbackRequest(
    @NotNull(message = "审核任务 ID 不能为空")
    Long taskId,
    @NotBlank(message = "审核状态不能为空")
    String reviewStatus,
    Map<String, Object> reviewResult,
    List<String> riskLabels,
    String failureReason,
    Map<String, Object> callbackPayload
) {
}
