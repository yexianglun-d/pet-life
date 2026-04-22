package com.petlife.server.modules.community.domain.entity;

import java.time.LocalDateTime;

/**
 * 社区举报实体。
 *
 * <p>举报记录是后台审核与处理动作的唯一事实来源。
 * 当前阶段先保证同一用户对同一帖子只保留一条 pending 举报，避免管理端被重复噪音淹没。</p>
 */
public final class CommunityReportEntity {

    private final Long reportId;
    private final Long reporterUserId;
    private final String targetType;
    private final Long targetId;
    private final String reasonCode;
    private final String reasonDetail;
    private final String status;
    private final LocalDateTime createdAt;

    public CommunityReportEntity(
        Long reportId,
        Long reporterUserId,
        String targetType,
        Long targetId,
        String reasonCode,
        String reasonDetail,
        String status,
        LocalDateTime createdAt
    ) {
        this.reportId = reportId;
        this.reporterUserId = reporterUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
