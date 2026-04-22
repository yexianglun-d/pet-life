package com.petlife.server.modules.moderation.dto.response;

import java.time.OffsetDateTime;

/**
 * 审核中心举报响应。
 *
 * @param reportId 举报 ID
 * @param targetType 举报对象类型
 * @param targetId 举报对象 ID
 * @param reasonCode 举报原因编码
 * @param reasonDetail 举报补充说明
 * @param status 举报状态
 * @param processedBy 处理人标识
 * @param processedAt 处理时间
 * @param createdAt 创建时间
 * @param reporterUserId 举报人用户 ID
 * @param reporterNickname 举报人昵称
 * @param reporterMobile 举报人手机号
 * @param postId 帖子 ID
 * @param postTitle 帖子标题
 * @param postContent 帖子正文
 * @param postReviewStatus 帖子审核状态
 * @param postVisibility 帖子可见性
 * @param postDeleted 帖子是否已撤回
 * @param postAuthorUserId 帖子作者用户 ID
 * @param postAuthorNickname 帖子作者昵称
 */
public record ModerationReportResponse(
    String reportId,
    String targetType,
    String targetId,
    String reasonCode,
    String reasonDetail,
    String status,
    String processedBy,
    OffsetDateTime processedAt,
    OffsetDateTime createdAt,
    String reporterUserId,
    String reporterNickname,
    String reporterMobile,
    String postId,
    String postTitle,
    String postContent,
    String postReviewStatus,
    String postVisibility,
    Boolean postDeleted,
    String postAuthorUserId,
    String postAuthorNickname
) {
}
