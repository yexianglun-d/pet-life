package com.petlife.server.modules.timeline.persistence;

import com.petlife.server.modules.timeline.persistence.command.DeleteTimelineEventCommand;
import com.petlife.server.modules.timeline.persistence.command.UpsertTimelineEventCommand;
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
