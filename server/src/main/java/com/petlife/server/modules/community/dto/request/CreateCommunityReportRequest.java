package com.petlife.server.modules.community.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建社区举报请求。
 *
 * @param reasonCode 举报原因编码
 * @param reasonDetail 举报补充说明
 */
public record CreateCommunityReportRequest(
    @NotBlank(message = "举报原因不能为空")
    String reasonCode,
    String reasonDetail
) {
}
