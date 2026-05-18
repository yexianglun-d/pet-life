package com.petlife.server.modules.moderation.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 审核中心举报数据对象。
 *
 * @param reportId 举报 ID
 * @param targetType 举报对象类型
 * @param targetId 举报对象 ID
 * @param reasonCode 举报原因编码
 * @param reasonDetail 举报补充说明
 * @param status 举报状态
 * @param processedBy 处理人标识
 * @param adminNotes 管理员处理备注
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
 * @param postDeletedAt 帖子删除时间
 * @param postAuthorUserId 帖子作者用户 ID
 * @param postAuthorNickname 帖子作者昵称
 */
public record ModerationReportDataObject(
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
    LocalDateTime postDeletedAt,
    Long postAuthorUserId,
    String postAuthorNickname
) {
}
