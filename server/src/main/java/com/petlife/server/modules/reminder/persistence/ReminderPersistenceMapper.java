package com.petlife.server.modules.reminder.persistence;

import com.petlife.server.modules.reminder.persistence.command.CreateReminderCommand;
import com.petlife.server.modules.reminder.persistence.command.HandleReminderCommand;
import com.petlife.server.modules.reminder.persistence.dataobject.ReminderDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
          reminder_mode AS reminderMode,
          cycle_value AS cycleValue,
          cycle_unit AS cycleUnit,
          remind_at AS dueAt,
          status AS status,
          NULL AS notes,
          handled_at AS handledAt,
          created_at AS createdAt
        FROM pet_reminders
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY
          CASE status WHEN 'pending' THEN 1 WHEN 'skipped' THEN 2 WHEN 'done' THEN 3 ELSE 4 END,
          remind_at ASC,
          id DESC
        """)
    List<ReminderDataObject> listRemindersByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS reminderId,
          pet_id AS petId,
          reminder_type AS reminderType,
          title AS title,
          reminder_mode AS reminderMode,
          cycle_value AS cycleValue,
          cycle_unit AS cycleUnit,
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
    ReminderDataObject findReminderById(@Param("reminderId") Long reminderId);

    @Select("""
        SELECT
          id AS reminderId,
          pet_id AS petId,
          reminder_type AS reminderType,
          title AS title,
          reminder_mode AS reminderMode,
          cycle_value AS cycleValue,
          cycle_unit AS cycleUnit,
          remind_at AS dueAt,
          status AS status,
          NULL AS notes,
          handled_at AS handledAt,
          created_at AS createdAt
        FROM pet_reminders
        WHERE id = #{reminderId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ReminderDataObject findReminderByPetIdAndId(
        @Param("petId") Long petId,
        @Param("reminderId") Long reminderId
    );

    @Insert("""
        INSERT INTO pet_reminders (
          pet_id, reminder_type, title, reminder_mode, cycle_value,
          cycle_unit, remind_at, status, created_at, updated_at
        ) VALUES (
          #{petId}, #{reminderType}, #{title}, #{reminderMode}, #{cycleValue},
          #{cycleUnit}, #{dueAt}, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReminder(CreateReminderCommand command);

    @Update("""
        UPDATE pet_reminders
        SET status = 'done',
            handled_at = CURRENT_TIMESTAMP,
            handler_user_id = #{handledByUserId},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{reminderId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
          AND status = 'pending'
        """)
    int completeReminder(
        HandleReminderCommand command
    );

    @Update("""
        UPDATE pet_reminders
        SET status = 'skipped',
            handled_at = CURRENT_TIMESTAMP,
            handler_user_id = #{handledByUserId},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{reminderId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
          AND status = 'pending'
        """)
    int skipReminder(
        HandleReminderCommand command
    );
}
