package com.petlife.server.modules.moderation.domain.entity;

import java.time.LocalDateTime;

/**
 * 审核中心举报实体。
 *
 * <p>后台处理举报时，需要同时看到举报记录和目标帖子当前状态。
 * 这里把举报与目标内容快照收敛成一个稳定实体，避免控制器直接依赖数据库联表结果。</p>
 */
public final class ModerationReportEntity {

    private final Long reportId;
    private final String targetType;
    private final Long targetId;
    private final String reasonCode;
    private final String reasonDetail;
    private final String status;
    private final String processedBy;
    private final String adminNotes;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;
    private final Long reporterUserId;
    private final String reporterNickname;
    private final String reporterMobile;
    private final Long postId;
    private final String postTitle;
    private final String postContent;
    private final String postReviewStatus;
    private final String postVisibility;
    private final boolean postDeleted;
    private final Long postAuthorUserId;
    private final String postAuthorNickname;

    public ModerationReportEntity(
        Long reportId,
        String targetType,
        Long targetId,
        String reasonCode,
        String reasonDetail,
        String status,
        String processedBy,
        String adminNotes,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        Long reporterUserId,
        String reporterNickname,
        String reporterMobile,
        Long postId,
        String postTitle,
        String postContent,
        String postReviewStatus,
        String postVisibility,
        boolean postDeleted,
        Long postAuthorUserId,
        String postAuthorNickname
    ) {
        this.reportId = reportId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.status = status;
        this.processedBy = processedBy;
        this.adminNotes = adminNotes;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.reporterUserId = reporterUserId;
        this.reporterNickname = reporterNickname;
        this.reporterMobile = reporterMobile;
        this.postId = postId;
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.postReviewStatus = postReviewStatus;
        this.postVisibility = postVisibility;
        this.postDeleted = postDeleted;
        this.postAuthorUserId = postAuthorUserId;
        this.postAuthorNickname = postAuthorNickname;
    }

    public Long getReportId() {
        return reportId;
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

    public String getProcessedBy() {
        return processedBy;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public String getReporterNickname() {
        return reporterNickname;
    }

    public String getReporterMobile() {
        return reporterMobile;
    }

    public Long getPostId() {
        return postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getPostContent() {
        return postContent;
    }

    public String getPostReviewStatus() {
        return postReviewStatus;
    }

    public String getPostVisibility() {
        return postVisibility;
    }

    public boolean isPostDeleted() {
        return postDeleted;
    }

    public Long getPostAuthorUserId() {
        return postAuthorUserId;
    }

    public String getPostAuthorNickname() {
        return postAuthorNickname;
    }
}
