package com.petlife.server.modules.dailylog.persistence;

import com.petlife.server.modules.dailylog.persistence.record.DailyLogPersistenceRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 萌宠日常持久化 Mapper。
 */
@Mapper
public interface DailyLogPersistenceMapper {

    @Select("""
        SELECT
          id AS dailyLogId,
          pet_id AS petId,
          author_user_id AS authorUserId,
          content AS content,
          JSON_UNQUOTE(JSON_EXTRACT(scene_tags, '$')) AS tagsJson,
          visibility AS visibility,
          happened_at AS happenedAt,
          created_at AS createdAt
        FROM pet_daily_logs
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY happened_at DESC, id DESC
        """)
    List<DailyLogPersistenceRecord> listDailyLogsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS dailyLogId,
          pet_id AS petId,
          author_user_id AS authorUserId,
          content AS content,
          JSON_UNQUOTE(JSON_EXTRACT(scene_tags, '$')) AS tagsJson,
          visibility AS visibility,
          happened_at AS happenedAt,
          created_at AS createdAt
        FROM pet_daily_logs
        WHERE id = #{dailyLogId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    DailyLogPersistenceRecord findDailyLogById(@Param("dailyLogId") Long dailyLogId);

    @Insert("""
        INSERT INTO pet_daily_logs (
          pet_id, author_user_id, content, scene_tags, visibility, happened_at,
          sync_to_timeline, created_at, updated_at
        ) VALUES (
          #{petId}, #{authorUserId}, #{content}, #{tagsJson}, #{visibility}, #{happenedAt},
          1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    int insertDailyLog(
        @Param("petId") Long petId,
        @Param("authorUserId") Long authorUserId,
        @Param("content") String content,
        @Param("tagsJson") String tagsJson,
        @Param("visibility") String visibility,
        @Param("happenedAt") LocalDateTime happenedAt
    );

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertId();
}
