package com.petlife.server.modules.timeline.persistence;

import com.petlife.server.modules.timeline.persistence.command.DeleteTimelineEventCommand;
import com.petlife.server.modules.timeline.persistence.command.UpsertTimelineEventCommand;
import com.petlife.server.modules.timeline.persistence.dataobject.AdminTimelineEventDataObject;
import com.petlife.server.modules.timeline.persistence.dataobject.TimelineEventDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 时间轴持久化 Mapper。
 */
@Mapper
public interface TimelinePersistenceMapper {

    @Select("""
        SELECT
          id AS eventId,
          pet_id AS petId,
          event_type AS eventType,
          source_type AS sourceType,
          source_id AS sourceId,
          event_time AS eventTime,
          title AS title,
          summary AS summary,
          cover_url AS coverUrl,
          visibility AS visibility,
          created_at AS createdAt
        FROM pet_timeline_events
        WHERE pet_id = #{petId}
        ORDER BY event_time DESC, id DESC
        """)
    List<TimelineEventDataObject> listTimelineEventsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS eventId,
          pet_id AS petId,
          event_type AS eventType,
          source_type AS sourceType,
          source_id AS sourceId,
          event_time AS eventTime,
          title AS title,
          summary AS summary,
          cover_url AS coverUrl,
          visibility AS visibility,
          created_at AS createdAt
        FROM pet_timeline_events
        WHERE pet_id = #{petId}
          AND event_type = #{eventType}
        ORDER BY event_time DESC, id DESC
        """)
    List<TimelineEventDataObject> listTimelineEventsByPetIdAndEventType(
        @Param("petId") Long petId,
        @Param("eventType") String eventType
    );

    @Select("""
        SELECT
          t.id AS eventId,
          t.pet_id AS petId,
          t.event_type AS eventType,
          t.source_type AS sourceType,
          t.source_id AS sourceId,
          t.event_time AS eventTime,
          t.title AS title,
          t.summary AS summary,
          t.cover_url AS coverUrl,
          t.visibility AS visibility,
          t.created_at AS createdAt,
          CASE
            WHEN t.source_type = 'health_record' AND h_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'health_record' AND h_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'health_record' THEN 'active'
            WHEN t.source_type = 'daily_log' AND d_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'daily_log' AND d_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'daily_log' THEN 'active'
            WHEN t.source_type = 'service_appointment' AND a_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'service_appointment' AND a_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'service_appointment' THEN 'active'
            ELSE 'unsupported'
          END AS sourceStatus,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile
        FROM pet_timeline_events t
        JOIN pets p ON p.id = t.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN pet_health_records h_source
          ON t.source_type = 'health_record'
          AND h_source.id = t.source_id
        LEFT JOIN pet_daily_logs d_source
          ON t.source_type = 'daily_log'
          AND d_source.id = t.source_id
        LEFT JOIN service_appointments a_source
          ON t.source_type = 'service_appointment'
          AND a_source.id = t.source_id
        WHERE p.deleted_at IS NULL
          AND (#{eventType} IS NULL OR t.event_type = #{eventType})
          AND (#{sourceType} IS NULL OR t.source_type = #{sourceType})
          AND (#{petId} IS NULL OR t.pet_id = #{petId})
          AND (#{sourceId} IS NULL OR t.source_id = #{sourceId})
        ORDER BY t.event_time DESC, t.id DESC
        LIMIT 200
        """)
    List<AdminTimelineEventDataObject> listAdminTimelineEvents(
        @Param("eventType") String eventType,
        @Param("sourceType") String sourceType,
        @Param("petId") Long petId,
        @Param("sourceId") Long sourceId
    );

    @Select("""
        SELECT
          t.id AS eventId,
          t.pet_id AS petId,
          t.event_type AS eventType,
          t.source_type AS sourceType,
          t.source_id AS sourceId,
          t.event_time AS eventTime,
          t.title AS title,
          t.summary AS summary,
          t.cover_url AS coverUrl,
          t.visibility AS visibility,
          t.created_at AS createdAt,
          CASE
            WHEN t.source_type = 'health_record' AND h_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'health_record' AND h_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'health_record' THEN 'active'
            WHEN t.source_type = 'daily_log' AND d_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'daily_log' AND d_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'daily_log' THEN 'active'
            WHEN t.source_type = 'service_appointment' AND a_source.id IS NULL THEN 'missing'
            WHEN t.source_type = 'service_appointment' AND a_source.deleted_at IS NOT NULL THEN 'deleted'
            WHEN t.source_type = 'service_appointment' THEN 'active'
            ELSE 'unsupported'
          END AS sourceStatus,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile
        FROM pet_timeline_events t
        JOIN pets p ON p.id = t.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN pet_health_records h_source
          ON t.source_type = 'health_record'
          AND h_source.id = t.source_id
        LEFT JOIN pet_daily_logs d_source
          ON t.source_type = 'daily_log'
          AND d_source.id = t.source_id
        LEFT JOIN service_appointments a_source
          ON t.source_type = 'service_appointment'
          AND a_source.id = t.source_id
        WHERE t.id = #{eventId}
          AND p.deleted_at IS NULL
        LIMIT 1
        """)
    AdminTimelineEventDataObject findAdminTimelineEventById(@Param("eventId") Long eventId);

    @Insert("""
        INSERT INTO pet_timeline_events (
          pet_id, event_type, source_type, source_id, event_time,
          title, summary, cover_url, visibility, created_at, updated_at
        ) VALUES (
          #{petId}, #{eventType}, #{sourceType}, #{sourceId}, #{eventTime},
          #{title}, #{summary}, #{coverUrl}, #{visibility}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE
          pet_id = VALUES(pet_id),
          event_type = VALUES(event_type),
          event_time = VALUES(event_time),
          title = VALUES(title),
          summary = VALUES(summary),
          cover_url = VALUES(cover_url),
          visibility = VALUES(visibility),
          updated_at = CURRENT_TIMESTAMP
        """)
    int upsertTimelineEvent(UpsertTimelineEventCommand command);

    @Delete("""
        DELETE FROM pet_timeline_events
        WHERE pet_id = #{petId}
          AND source_type = #{sourceType}
          AND source_id = #{sourceId}
        """)
    int deleteTimelineEvent(DeleteTimelineEventCommand command);
}
