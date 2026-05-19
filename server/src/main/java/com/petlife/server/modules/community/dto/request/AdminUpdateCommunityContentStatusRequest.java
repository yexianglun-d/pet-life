package com.petlife.server.modules.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台社区内容治理状态请求。
 *
 * @param action 治理动作：take_down/restore
 * @param adminNotes 管理员备注
 */
public record AdminUpdateCommunityContentStatusRequest(
    @NotBlank(message = "治理动作不能为空")
    String action,
    @Size(max = 500, message = "管理员备注不能超过 500 字")
    String adminNotes
) {
}
