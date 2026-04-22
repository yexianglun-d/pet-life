package com.petlife.server.modules.community.dto.response;

import java.time.OffsetDateTime;

/**
 * 社区举报响应。
 *
 * @param reportId 举报 ID
 * @param targetType 举报对象类型
 * @param targetId 举报对象 ID
 * @param reasonCode 举报原因编码
 * @param reasonDetail 举报补充说明
 * @param status 举报状态
 * @param createdAt 创建时间
 */
public record CommunityReportResponse(
    String reportId,
    String targetType,
    String targetId,
    String reasonCode,
    String reasonDetail,
    String status,
    OffsetDateTime createdAt
) {
}
