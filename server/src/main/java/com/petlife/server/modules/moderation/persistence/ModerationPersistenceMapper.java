package com.petlife.server.modules.moderation.persistence;

import com.petlife.server.modules.moderation.persistence.command.ProcessModerationReportCommand;
import com.petlife.server.modules.moderation.persistence.command.UpdateModerationTargetPostReviewStatusCommand;
import com.petlife.server.modules.moderation.persistence.dataobject.ModerationReportDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 审核中心持久化 Mapper。
 */
@Mapper
public interface ModerationPersistenceMapper {

    @Select("""
        SELECT
          cr.id AS reportId,
          cr.target_type AS targetType,
          cr.target_id AS targetId,
          cr.reason_code AS reasonCode,
          cr.reason_detail AS reasonDetail,
          cr.status AS status,
          cr.processed_by AS processedBy,
          cr.admin_notes AS adminNotes,
          cr.processed_at AS processedAt,
          cr.created_at AS createdAt,
          reporter.id AS reporterUserId,
          reporter.nickname AS reporterNickname,
          reporter.mobile AS reporterMobile,
          cp.id AS postId,
          cp.title AS postTitle,
          cp.content AS postContent,
          cp.review_status AS postReviewStatus,
          cp.visibility AS postVisibility,
          cp.deleted_at AS postDeletedAt,
          author.id AS postAuthorUserId,
          author.nickname AS postAuthorNickname
        FROM community_reports cr
        JOIN users reporter ON reporter.id = cr.reporter_user_id
        LEFT JOIN community_posts cp
          ON cr.target_type = 'post'
         AND cp.id = cr.target_id
        LEFT JOIN users author ON author.id = cp.user_id
        WHERE (#{status} IS NULL OR cr.status = #{status})
        ORDER BY
          CASE WHEN cr.status = 'pending' THEN 0 ELSE 1 END,
          cr.created_at DESC,
          cr.id DESC
        """)
    List<ModerationReportDataObject> listReports(@Param("status") String status);

    @Select("""
        SELECT
          cr.id AS reportId,
          cr.target_type AS targetType,
          cr.target_id AS targetId,
          cr.reason_code AS reasonCode,
          cr.reason_detail AS reasonDetail,
          cr.status AS status,
          cr.processed_by AS processedBy,
          cr.admin_notes AS adminNotes,
          cr.processed_at AS processedAt,
          cr.created_at AS createdAt,
          reporter.id AS reporterUserId,
          reporter.nickname AS reporterNickname,
          reporter.mobile AS reporterMobile,
          cp.id AS postId,
          cp.title AS postTitle,
          cp.content AS postContent,
          cp.review_status AS postReviewStatus,
          cp.visibility AS postVisibility,
          cp.deleted_at AS postDeletedAt,
          author.id AS postAuthorUserId,
          author.nickname AS postAuthorNickname
        FROM community_reports cr
        JOIN users reporter ON reporter.id = cr.reporter_user_id
        LEFT JOIN community_posts cp
          ON cr.target_type = 'post'
         AND cp.id = cr.target_id
        LEFT JOIN users author ON author.id = cp.user_id
        WHERE cr.id = #{reportId}
        LIMIT 1
        """)
    ModerationReportDataObject findReportById(@Param("reportId") Long reportId);

    @Update("""
        UPDATE community_reports
        SET status = #{status},
            processed_by = #{processedBy},
            admin_notes = #{adminNotes},
            processed_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{reportId}
          AND status = 'pending'
        """)
    int processReport(ProcessModerationReportCommand command);

    @Update("""
        UPDATE community_posts
        SET review_status = #{reviewStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{postId}
          AND deleted_at IS NULL
        """)
    int updateTargetPostReviewStatus(UpdateModerationTargetPostReviewStatusCommand command);
}
