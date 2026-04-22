package com.petlife.server.modules.community.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 社区举报数据对象。
 *
 * @param reportId 举报 ID
 * @param reporterUserId 举报用户 ID
 * @param targetType 举报对象类型
 * @param targetId 举报对象 ID
 * @param reasonCode 举报原因编码
 * @param reasonDetail 举报补充说明
 * @param status 举报状态
 * @param createdAt 创建时间
 */
public record CommunityReportDataObject(
    Long reportId,
    Long reporterUserId,
    String targetType,
    Long targetId,
    String reasonCode,
    String reasonDetail,
    String status,
    LocalDateTime createdAt
) {
}
