package com.petlife.server.modules.dailylog.persistence;

import com.petlife.server.modules.dailylog.persistence.command.CreateDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.DeleteDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.UpdateDailyLogCommunityBindingCommand;
import com.petlife.server.modules.dailylog.persistence.command.UpdateDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.dataobject.AdminDailyLogDataObject;
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
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(media_list, JSON_ARRAY()), '$')) AS mediaListJson,
          JSON_UNQUOTE(JSON_EXTRACT(scene_tags, '$')) AS tagsJson,
          visibility AS visibility,
          sync_to_community AS syncToCommunity,
          community_post_id AS communityPostId,
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
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(media_list, JSON_ARRAY()), '$')) AS mediaListJson,
          JSON_UNQUOTE(JSON_EXTRACT(scene_tags, '$')) AS tagsJson,
          visibility AS visibility,
          sync_to_community AS syncToCommunity,
          community_post_id AS communityPostId,
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
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(media_list, JSON_ARRAY()), '$')) AS mediaListJson,
          JSON_UNQUOTE(JSON_EXTRACT(scene_tags, '$')) AS tagsJson,
          visibility AS visibility,
          sync_to_community AS syncToCommunity,
          community_post_id AS communityPostId,
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

    @Select("""
        SELECT
          d.id AS dailyLogId,
          d.pet_id AS petId,
          d.author_user_id AS authorUserId,
          d.content AS content,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(d.media_list, JSON_ARRAY()), '$')) AS mediaListJson,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(d.scene_tags, JSON_ARRAY()), '$')) AS tagsJson,
          d.visibility AS visibility,
          d.sync_to_community AS syncToCommunity,
          d.community_post_id AS communityPostId,
          d.happened_at AS happenedAt,
          d.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          author.nickname AS authorNickname,
          author.mobile AS authorMobile
        FROM pet_daily_logs d
        JOIN pets p ON p.id = d.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN users author ON author.id = d.author_user_id
        WHERE d.deleted_at IS NULL
          AND p.deleted_at IS NULL
          AND (#{visibility} IS NULL OR d.visibility = #{visibility})
          AND (#{syncToCommunity} IS NULL OR d.sync_to_community = #{syncToCommunity})
          AND (#{petId} IS NULL OR d.pet_id = #{petId})
          AND (#{authorUserId} IS NULL OR d.author_user_id = #{authorUserId})
          AND (
            #{keyword} IS NULL
            OR d.content LIKE CONCAT('%', #{keyword}, '%')
            OR p.pet_name LIKE CONCAT('%', #{keyword}, '%')
            OR owner.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR owner.mobile LIKE CONCAT('%', #{keyword}, '%')
            OR author.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR author.mobile LIKE CONCAT('%', #{keyword}, '%')
          )
        ORDER BY d.happened_at DESC, d.id DESC
        LIMIT 200
        """)
    List<AdminDailyLogDataObject> listAdminDailyLogs(
        @Param("visibility") String visibility,
        @Param("syncToCommunity") Boolean syncToCommunity,
        @Param("petId") Long petId,
        @Param("authorUserId") Long authorUserId,
        @Param("keyword") String keyword
    );

    @Select("""
        SELECT
          d.id AS dailyLogId,
          d.pet_id AS petId,
          d.author_user_id AS authorUserId,
          d.content AS content,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(d.media_list, JSON_ARRAY()), '$')) AS mediaListJson,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(d.scene_tags, JSON_ARRAY()), '$')) AS tagsJson,
          d.visibility AS visibility,
          d.sync_to_community AS syncToCommunity,
          d.community_post_id AS communityPostId,
          d.happened_at AS happenedAt,
          d.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          author.nickname AS authorNickname,
          author.mobile AS authorMobile
        FROM pet_daily_logs d
        JOIN pets p ON p.id = d.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN users author ON author.id = d.author_user_id
        WHERE d.id = #{dailyLogId}
          AND d.deleted_at IS NULL
          AND p.deleted_at IS NULL
        LIMIT 1
        """)
    AdminDailyLogDataObject findAdminDailyLogById(@Param("dailyLogId") Long dailyLogId);

    @Insert("""
        INSERT INTO pet_daily_logs (
          pet_id, author_user_id, content, media_list, scene_tags, visibility, sync_to_community, happened_at,
          sync_to_timeline, created_at, updated_at
        ) VALUES (
          #{petId}, #{authorUserId}, #{content}, #{mediaListJson}, #{tagsJson}, #{visibility}, #{syncToCommunity}, #{happenedAt},
          1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDailyLog(CreateDailyLogCommand command);

    @Update("""
        UPDATE pet_daily_logs
        SET content = #{content},
            media_list = #{mediaListJson},
            scene_tags = #{tagsJson},
            visibility = #{visibility},
            sync_to_community = #{syncToCommunity},
            happened_at = #{happenedAt},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{dailyLogId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        """)
    int updateDailyLog(UpdateDailyLogCommand command);

    @Update("""
        UPDATE pet_daily_logs
        SET community_post_id = #{communityPostId},
            sync_to_community = #{syncToCommunity},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{dailyLogId}
          AND deleted_at IS NULL
        """)
    int updateCommunityBinding(UpdateDailyLogCommunityBindingCommand command);

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
