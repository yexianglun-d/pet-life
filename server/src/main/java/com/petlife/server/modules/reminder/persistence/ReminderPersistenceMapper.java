package com.petlife.server.modules.reminder.persistence;

import com.petlife.server.modules.reminder.persistence.record.ReminderPersistenceRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 宠物提醒持久化 Mapper。
 */
@Mapper
public interface ReminderPersistenceMapper {

    @Select("""
        SELECT
          id AS reminderId,
          pet_id AS petId,
          reminder_type AS reminderType,
          title AS title,
          remind_at AS dueAt,
          status AS status,
          NULL AS notes,
          handled_at AS handledAt,
          created_at AS createdAt
        FROM pet_reminders
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY
          CASE status WHEN 'pending' THEN 1 WHEN 'completed' THEN 2 ELSE 3 END,
          remind_at ASC,
          id DESC
        """)
    List<ReminderPersistenceRecord> listRemindersByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS reminderId,
          pet_id AS petId,
          reminder_type AS reminderType,
          title AS title,
          remind_at AS dueAt,
          status AS status,
          NULL AS notes,
          handled_at AS handledAt,
          created_at AS createdAt
        FROM pet_reminders
        WHERE id = #{reminderId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ReminderPersistenceRecord findReminderById(@Param("reminderId") Long reminderId);

    @Insert("""
        INSERT INTO pet_reminders (
          pet_id, reminder_type, title, remind_at, reminder_mode,
          status, created_at, updated_at
        ) VALUES (
          #{petId}, #{reminderType}, #{title}, #{dueAt}, 'single',
          'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    int insertReminder(
        @Param("petId") Long petId,
        @Param("reminderType") String reminderType,
        @Param("title") String title,
        @Param("dueAt") LocalDateTime dueAt,
        @Param("notes") String notes
    );

    @Update("""
        UPDATE pet_reminders
        SET status = 'done',
            handled_at = CURRENT_TIMESTAMP,
            handler_user_id = #{completedByUserId},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{reminderId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
          AND status <> 'done'
        """)
    int completeReminder(
        @Param("petId") Long petId,
        @Param("reminderId") Long reminderId,
        @Param("completedByUserId") Long completedByUserId
    );

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertId();
}
