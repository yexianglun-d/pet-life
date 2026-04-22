package com.petlife.server.modules.dailylog.persistence;

import com.petlife.server.modules.dailylog.persistence.command.CreateDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.DeleteDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.UpdateDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.dataobject.DailyLogDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    List<DailyLogDataObject> listDailyLogsByPetId(@Param("petId") Long petId);

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
    DailyLogDataObject findDailyLogById(@Param("dailyLogId") Long dailyLogId);

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
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    DailyLogDataObject findDailyLogByPetIdAndId(
        @Param("petId") Long petId,
        @Param("dailyLogId") Long dailyLogId
    );

    @Insert("""
        INSERT INTO pet_daily_logs (
          pet_id, author_user_id, content, scene_tags, visibility, happened_at,
          sync_to_timeline, created_at, updated_at
        ) VALUES (
          #{petId}, #{authorUserId}, #{content}, #{tagsJson}, #{visibility}, #{happenedAt},
          1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDailyLog(CreateDailyLogCommand command);

    @Update("""
        UPDATE pet_daily_logs
        SET content = #{content},
            scene_tags = #{tagsJson},
            visibility = #{visibility},
            happened_at = #{happenedAt},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{dailyLogId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        """)
    int updateDailyLog(UpdateDailyLogCommand command);

    @Update("""
        UPDATE pet_daily_logs
        SET deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{dailyLogId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        """)
    int deleteDailyLog(DeleteDailyLogCommand command);
}
