package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建服务商评价请求。
 */
public record CreateProviderReviewRequest(
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于 1")
    @Max(value = 5, message = "评分不能高于 5")
    Integer rating,
    @Size(max = 1000, message = "评价内容不能超过 1000 个字符")
    String content
) {
}
