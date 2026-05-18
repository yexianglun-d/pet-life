package com.petlife.server.modules.reminder.persistence;

import com.petlife.server.modules.reminder.persistence.command.CreateReminderCommand;
import com.petlife.server.modules.reminder.persistence.command.HandleReminderCommand;
import com.petlife.server.modules.reminder.persistence.dataobject.AdminReminderDataObject;
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

    @Select("""
        SELECT
          r.id AS reminderId,
          r.pet_id AS petId,
          r.reminder_type AS reminderType,
          r.title AS title,
          r.reminder_mode AS reminderMode,
          r.cycle_value AS cycleValue,
          r.cycle_unit AS cycleUnit,
          r.remind_at AS dueAt,
          r.status AS status,
          NULL AS notes,
          r.handled_at AS handledAt,
          r.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          handler.id AS handlerUserId,
          handler.nickname AS handlerNickname,
          handler.mobile AS handlerMobile,
          r.source_record_id AS sourceRecordId,
          source_record.record_type AS sourceRecordType,
          source_record.title AS sourceRecordTitle,
          CASE
            WHEN r.source_record_id IS NULL THEN NULL
            WHEN source_record.id IS NULL THEN 'missing'
            WHEN source_record.deleted_at IS NULL THEN 'active'
            ELSE 'deleted'
          END AS sourceRecordStatus
        FROM pet_reminders r
        JOIN pets p ON p.id = r.pet_id
        LEFT JOIN families f
          ON f.id = p.family_id
         AND f.deleted_at IS NULL
        LEFT JOIN users owner
          ON owner.id = p.owner_user_id
         AND owner.deleted_at IS NULL
        LEFT JOIN users handler
          ON handler.id = r.handler_user_id
         AND handler.deleted_at IS NULL
        LEFT JOIN pet_health_records source_record ON source_record.id = r.source_record_id
        WHERE r.deleted_at IS NULL
          AND p.deleted_at IS NULL
          AND (
            #{keyword} IS NULL
            OR r.title LIKE CONCAT('%', #{keyword}, '%')
            OR p.pet_name LIKE CONCAT('%', #{keyword}, '%')
            OR f.family_name LIKE CONCAT('%', #{keyword}, '%')
            OR owner.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR owner.mobile LIKE CONCAT('%', #{keyword}, '%')
            OR source_record.title LIKE CONCAT('%', #{keyword}, '%')
          )
          AND (#{status} IS NULL OR r.status = #{status})
          AND (#{reminderType} IS NULL OR r.reminder_type = #{reminderType})
          AND (#{reminderMode} IS NULL OR r.reminder_mode = #{reminderMode})
          AND (#{petId} IS NULL OR r.pet_id = #{petId})
          AND (#{familyId} IS NULL OR p.family_id = #{familyId})
          AND (#{ownerUserId} IS NULL OR p.owner_user_id = #{ownerUserId})
          AND (#{handlerUserId} IS NULL OR r.handler_user_id = #{handlerUserId})
          AND (#{sourceRecordId} IS NULL OR r.source_record_id = #{sourceRecordId})
          AND (#{dueFrom} IS NULL OR r.remind_at >= #{dueFrom})
          AND (#{dueTo} IS NULL OR r.remind_at <= #{dueTo})
        ORDER BY r.remind_at DESC, r.id DESC
        LIMIT 200
        """)
    List<AdminReminderDataObject> listAdminReminders(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("reminderType") String reminderType,
        @Param("reminderMode") String reminderMode,
        @Param("petId") Long petId,
        @Param("familyId") Long familyId,
        @Param("ownerUserId") Long ownerUserId,
        @Param("handlerUserId") Long handlerUserId,
        @Param("sourceRecordId") Long sourceRecordId,
        @Param("dueFrom") java.time.LocalDateTime dueFrom,
        @Param("dueTo") java.time.LocalDateTime dueTo
    );

    @Select("""
        SELECT
          r.id AS reminderId,
          r.pet_id AS petId,
          r.reminder_type AS reminderType,
          r.title AS title,
          r.reminder_mode AS reminderMode,
          r.cycle_value AS cycleValue,
          r.cycle_unit AS cycleUnit,
          r.remind_at AS dueAt,
          r.status AS status,
          NULL AS notes,
          r.handled_at AS handledAt,
          r.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          handler.id AS handlerUserId,
          handler.nickname AS handlerNickname,
          handler.mobile AS handlerMobile,
          r.source_record_id AS sourceRecordId,
          source_record.record_type AS sourceRecordType,
          source_record.title AS sourceRecordTitle,
          CASE
            WHEN r.source_record_id IS NULL THEN NULL
            WHEN source_record.id IS NULL THEN 'missing'
            WHEN source_record.deleted_at IS NULL THEN 'active'
            ELSE 'deleted'
          END AS sourceRecordStatus
        FROM pet_reminders r
        JOIN pets p ON p.id = r.pet_id
        LEFT JOIN families f
          ON f.id = p.family_id
         AND f.deleted_at IS NULL
        LEFT JOIN users owner
          ON owner.id = p.owner_user_id
         AND owner.deleted_at IS NULL
        LEFT JOIN users handler
          ON handler.id = r.handler_user_id
         AND handler.deleted_at IS NULL
        LEFT JOIN pet_health_records source_record ON source_record.id = r.source_record_id
        WHERE r.id = #{reminderId}
          AND r.deleted_at IS NULL
          AND p.deleted_at IS NULL
        LIMIT 1
        """)
    AdminReminderDataObject findAdminReminderById(@Param("reminderId") Long reminderId);

    @Insert("""
        INSERT INTO pet_reminders (
          pet_id, reminder_type, title, reminder_mode, cycle_value,
          cycle_unit, remind_at, status, source_record_id, created_at, updated_at
        ) VALUES (
          #{petId}, #{reminderType}, #{title}, #{reminderMode}, #{cycleValue},
          #{cycleUnit}, #{dueAt}, 'pending', #{sourceRecordId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
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

    @Update("""
        UPDATE pet_reminders
        SET deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE source_record_id = #{sourceRecordId}
          AND status = 'pending'
          AND deleted_at IS NULL
        """)
    int deletePendingRemindersBySourceRecordId(@Param("sourceRecordId") Long sourceRecordId);
}
