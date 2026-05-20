package com.petlife.server.modules.moderation.persistence;

import com.petlife.server.modules.moderation.persistence.command.CreateModerationTaskCommand;
import com.petlife.server.modules.moderation.persistence.command.UpdateModerationTaskReviewCommand;
import com.petlife.server.modules.moderation.persistence.dataobject.ModerationTaskDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 内容审核任务持久化 Mapper。
 */
@Mapper
public interface ModerationTaskPersistenceMapper {

    String TASK_SELECT_COLUMNS = """
          id AS taskId,
          target_type AS targetType,
          target_id AS targetId,
          content_type AS contentType,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(content_snapshot, JSON_OBJECT()), '$')) AS contentSnapshot,
          provider_code AS providerCode,
          review_status AS reviewStatus,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(review_result, JSON_OBJECT()), '$')) AS reviewResult,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(risk_labels, JSON_ARRAY()), '$')) AS riskLabels,
          failure_reason AS failureReason,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(callback_payload, JSON_OBJECT()), '$')) AS callbackPayload,
          reviewed_at AS reviewedAt,
          created_at AS createdAt,
          updated_at AS updatedAt
        """;

    @Select("""
        SELECT
        """ + TASK_SELECT_COLUMNS + """
        FROM moderation_tasks
        WHERE (#{targetType} IS NULL OR target_type = #{targetType})
          AND (#{targetId} IS NULL OR target_id = #{targetId})
          AND (#{contentType} IS NULL OR content_type = #{contentType})
          AND (#{reviewStatus} IS NULL OR review_status = #{reviewStatus})
          AND (#{providerCode} IS NULL OR provider_code = #{providerCode})
        ORDER BY
          CASE WHEN review_status = 'pending' THEN 0 ELSE 1 END,
          created_at DESC,
          id DESC
        LIMIT 200
        """)
    List<ModerationTaskDataObject> listTasks(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("contentType") String contentType,
        @Param("reviewStatus") String reviewStatus,
        @Param("providerCode") String providerCode
    );

    @Select("""
        SELECT
        """ + TASK_SELECT_COLUMNS + """
        FROM moderation_tasks
        WHERE id = #{taskId}
        LIMIT 1
        """)
    ModerationTaskDataObject findTaskById(@Param("taskId") Long taskId);

    @Select("""
        SELECT
        """ + TASK_SELECT_COLUMNS + """
        FROM moderation_tasks
        WHERE target_type = #{targetType}
          AND target_id = #{targetId}
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """)
    ModerationTaskDataObject findLatestTaskByTarget(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    @Insert("""
        INSERT INTO moderation_tasks (
          target_type, target_id, content_type, content_snapshot, provider_code,
          review_status, review_result, risk_labels, failure_reason, callback_payload,
          reviewed_at, created_at, updated_at
        ) VALUES (
          #{targetType}, #{targetId}, #{contentType}, #{contentSnapshot}, #{providerCode},
          #{reviewStatus}, #{reviewResult}, #{riskLabels}, #{failureReason},
          #{callbackPayload},
          CASE WHEN #{reviewStatus} IN ('approved', 'rejected', 'failed') THEN CURRENT_TIMESTAMP ELSE NULL END,
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "taskId")
    int insertTask(CreateModerationTaskCommand command);

    @Update("""
        UPDATE moderation_tasks
        SET review_status = #{reviewStatus},
            review_result = #{reviewResult},
            risk_labels = #{riskLabels},
            failure_reason = #{failureReason},
            callback_payload = #{callbackPayload},
            reviewed_at = CASE WHEN #{reviewStatus} IN ('approved', 'rejected', 'failed') THEN CURRENT_TIMESTAMP ELSE reviewed_at END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{taskId}
          AND review_status = 'pending'
        """)
    int updatePendingTaskReview(UpdateModerationTaskReviewCommand command);

    @Update("""
        UPDATE moderation_tasks
        SET review_status = 'failed',
            failure_reason = #{failureReason},
            reviewed_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE target_type = #{targetType}
          AND target_id = #{targetId}
          AND review_status = 'pending'
        """)
    int failPendingTasksByTarget(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("failureReason") String failureReason
    );
}
