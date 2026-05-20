package com.petlife.server.modules.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 后台人工处理审核任务请求。
 *
 * @param action 处理动作：approve/reject
 * @param riskLabels 风险标签
 * @param adminNotes 管理员备注
 */
public record AdminReviewModerationTaskRequest(
    @NotBlank(message = "审核处理动作不能为空")
    String action,
    List<String> riskLabels,
    @Size(max = 500, message = "管理员备注不能超过 500 字")
    String adminNotes
) {
}
