package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台更新服务评价状态请求。
 */
public record AdminUpdateProviderReviewStatusRequest(
    @NotBlank(message = "评价状态不能为空")
    String status
) {
}
