package com.petlife.server.modules.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 举报处理请求。
 *
 * @param action 处理动作
 */
public record ProcessModerationReportRequest(
    @NotBlank(message = "处理动作不能为空")
    String action
) {
}
