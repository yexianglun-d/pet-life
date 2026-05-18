package com.petlife.server.modules.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 举报处理请求。
 *
 * @param action 处理动作
 * @param adminNotes 管理员备注
 */
public record ProcessModerationReportRequest(
    @NotBlank(message = "处理动作不能为空")
    String action,
    @Size(max = 500, message = "管理员备注不能超过 500 字")
    String adminNotes
) {
}
