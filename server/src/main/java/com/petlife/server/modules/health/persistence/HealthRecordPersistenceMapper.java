package com.petlife.server.modules.health.persistence;

import com.petlife.server.modules.health.persistence.command.CreateHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.DeleteHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.UpdateHealthRecordCommand;
import com.petlife.server.modules.health.persistence.dataobject.AdminHealthRecordDataObject;
import com.petlife.server.modules.health.persistence.dataobject.HealthRecordDataObject;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 宠物健康记录持久化 Mapper。
 */
@Mapper
public interface HealthRecordPersistenceMapper {

    @Select("""
        SELECT
          h.id AS healthRecordId,
          h.pet_id AS petId,
          h.operator_user_id AS operatorUserId,
          h.record_type AS recordType,
          h.title AS title,
          h.occurred_at AS occurredAt,
          h.hospital_name AS hospitalName,
          h.doctor_name AS doctorName,
          h.severity_level AS severityLevel,
          h.result_summary AS resultSummary,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(h.attachments, JSON_ARRAY()), '$')) AS attachments,
          source_reminder.id AS nextReminderId,
          source_reminder.remind_at AS nextReminderAt,
          source_reminder.status AS nextReminderStatus,
          h.notes AS notes,
          h.created_at AS createdAt
        FROM pet_health_records h
        LEFT JOIN pet_reminders source_reminder
          ON source_reminder.id = (
            SELECT r.id
            FROM pet_reminders r
            WHERE r.source_record_id = h.id
              AND r.deleted_at IS NULL
            ORDER BY CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                     r.remind_at DESC,
                     r.id DESC
            LIMIT 1
          )
        WHERE h.pet_id = #{petId}
          AND h.deleted_at IS NULL
        ORDER BY h.occurred_at DESC, h.id DESC
        """)
    List<HealthRecordDataObject> listHealthRecordsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          h.id AS healthRecordId,
          h.pet_id AS petId,
          h.operator_user_id AS operatorUserId,
          h.record_type AS recordType,
          h.title AS title,
          h.occurred_at AS occurredAt,
          h.hospital_name AS hospitalName,
          h.doctor_name AS doctorName,
          h.severity_level AS severityLevel,
          h.result_summary AS resultSummary,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(h.attachments, JSON_ARRAY()), '$')) AS attachments,
          source_reminder.id AS nextReminderId,
          source_reminder.remind_at AS nextReminderAt,
          source_reminder.status AS nextReminderStatus,
          h.notes AS notes,
          h.created_at AS createdAt
        FROM pet_health_records h
        LEFT JOIN pet_reminders source_reminder
          ON source_reminder.id = (
            SELECT r.id
            FROM pet_reminders r
            WHERE r.source_record_id = h.id
              AND r.deleted_at IS NULL
            ORDER BY CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                     r.remind_at DESC,
                     r.id DESC
            LIMIT 1
          )
        WHERE h.id = #{healthRecordId}
          AND h.deleted_at IS NULL
        LIMIT 1
        """)
    HealthRecordDataObject findHealthRecordById(@Param("healthRecordId") Long healthRecordId);

    @Select("""
        SELECT
          h.id AS healthRecordId,
          h.pet_id AS petId,
          h.operator_user_id AS operatorUserId,
          h.record_type AS recordType,
          h.title AS title,
          h.occurred_at AS occurredAt,
          h.hospital_name AS hospitalName,
          h.doctor_name AS doctorName,
          h.severity_level AS severityLevel,
          h.result_summary AS resultSummary,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(h.attachments, JSON_ARRAY()), '$')) AS attachments,
          source_reminder.id AS nextReminderId,
          source_reminder.remind_at AS nextReminderAt,
          source_reminder.status AS nextReminderStatus,
          h.notes AS notes,
          h.created_at AS createdAt
        FROM pet_health_records h
        LEFT JOIN pet_reminders source_reminder
          ON source_reminder.id = (
            SELECT r.id
            FROM pet_reminders r
            WHERE r.source_record_id = h.id
              AND r.deleted_at IS NULL
            ORDER BY CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                     r.remind_at DESC,
                     r.id DESC
            LIMIT 1
          )
        WHERE h.pet_id = #{petId}
          AND h.id = #{healthRecordId}
          AND h.deleted_at IS NULL
        LIMIT 1
        """)
    HealthRecordDataObject findHealthRecordByPetIdAndId(
        @Param("petId") Long petId,
        @Param("healthRecordId") Long healthRecordId
    );

    @Select("""
        SELECT
          h.id AS healthRecordId,
          h.pet_id AS petId,
          h.operator_user_id AS operatorUserId,
          h.record_type AS recordType,
          h.title AS title,
          h.occurred_at AS occurredAt,
          h.hospital_name AS hospitalName,
          h.doctor_name AS doctorName,
          h.severity_level AS severityLevel,
          h.result_summary AS resultSummary,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(h.attachments, JSON_ARRAY()), '$')) AS attachments,
          source_reminder.id AS nextReminderId,
          source_reminder.remind_at AS nextReminderAt,
          source_reminder.status AS nextReminderStatus,
          h.notes AS notes,
          h.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          operator.nickname AS operatorNickname,
          operator.mobile AS operatorMobile
        FROM pet_health_records h
        JOIN pets p ON p.id = h.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN users operator ON operator.id = h.operator_user_id
        LEFT JOIN pet_reminders source_reminder
          ON source_reminder.id = (
            SELECT r.id
            FROM pet_reminders r
            WHERE r.source_record_id = h.id
              AND r.deleted_at IS NULL
            ORDER BY CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                     r.remind_at DESC,
                     r.id DESC
            LIMIT 1
          )
        WHERE h.deleted_at IS NULL
          AND p.deleted_at IS NULL
          AND (#{recordType} IS NULL OR h.record_type = #{recordType})
          AND (#{petId} IS NULL OR h.pet_id = #{petId})
          AND (#{operatorUserId} IS NULL OR h.operator_user_id = #{operatorUserId})
          AND (
            #{keyword} IS NULL
            OR h.title LIKE CONCAT('%', #{keyword}, '%')
            OR h.notes LIKE CONCAT('%', #{keyword}, '%')
            OR p.pet_name LIKE CONCAT('%', #{keyword}, '%')
            OR owner.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR owner.mobile LIKE CONCAT('%', #{keyword}, '%')
          )
        ORDER BY h.occurred_at DESC, h.id DESC
        LIMIT 200
        """)
    List<AdminHealthRecordDataObject> listAdminHealthRecords(
        @Param("recordType") String recordType,
        @Param("petId") Long petId,
        @Param("operatorUserId") Long operatorUserId,
        @Param("keyword") String keyword
    );

    @Select("""
        SELECT
          h.id AS healthRecordId,
          h.pet_id AS petId,
          h.operator_user_id AS operatorUserId,
          h.record_type AS recordType,
          h.title AS title,
          h.occurred_at AS occurredAt,
          h.hospital_name AS hospitalName,
          h.doctor_name AS doctorName,
          h.severity_level AS severityLevel,
          h.result_summary AS resultSummary,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(h.attachments, JSON_ARRAY()), '$')) AS attachments,
          source_reminder.id AS nextReminderId,
          source_reminder.remind_at AS nextReminderAt,
          source_reminder.status AS nextReminderStatus,
          h.notes AS notes,
          h.created_at AS createdAt,
          p.pet_name AS petName,
          p.pet_type AS petType,
          f.id AS familyId,
          f.family_name AS familyName,
          owner.id AS ownerUserId,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile,
          operator.nickname AS operatorNickname,
          operator.mobile AS operatorMobile
        FROM pet_health_records h
        JOIN pets p ON p.id = h.pet_id
        LEFT JOIN families f ON f.id = p.family_id
        LEFT JOIN users owner ON owner.id = p.owner_user_id
        LEFT JOIN users operator ON operator.id = h.operator_user_id
        LEFT JOIN pet_reminders source_reminder
          ON source_reminder.id = (
            SELECT r.id
            FROM pet_reminders r
            WHERE r.source_record_id = h.id
              AND r.deleted_at IS NULL
            ORDER BY CASE WHEN r.status = 'pending' THEN 0 ELSE 1 END,
                     r.remind_at DESC,
                     r.id DESC
            LIMIT 1
          )
        WHERE h.id = #{healthRecordId}
          AND h.deleted_at IS NULL
          AND p.deleted_at IS NULL
        LIMIT 1
        """)
    AdminHealthRecordDataObject findAdminHealthRecordById(@Param("healthRecordId") Long healthRecordId);

    @Insert("""
        INSERT INTO pet_health_records (
          pet_id, operator_user_id, record_type, title, occurred_at, result_summary,
          hospital_name, doctor_name, severity_level, attachments, notes, created_at, updated_at
        ) VALUES (
          #{petId}, #{operatorUserId}, #{recordType}, #{title}, #{occurredAt}, #{resultSummary},
          #{hospitalName}, #{doctorName}, #{severityLevel}, #{attachmentsJson},
          #{notes}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertHealthRecord(CreateHealthRecordCommand command);

    @Update("""
        UPDATE pet_health_records
        SET operator_user_id = #{operatorUserId},
            record_type = #{recordType},
            title = #{title},
            occurred_at = #{occurredAt},
            hospital_name = #{hospitalName},
            doctor_name = #{doctorName},
            severity_level = #{severityLevel},
            result_summary = #{resultSummary},
            attachments = #{attachmentsJson},
            notes = #{notes},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{healthRecordId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        """)
    int updateHealthRecord(UpdateHealthRecordCommand command);

    @Update("""
        UPDATE pet_health_records
        SET deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{healthRecordId}
          AND pet_id = #{petId}
          AND deleted_at IS NULL
        """)
    int deleteHealthRecord(DeleteHealthRecordCommand command);
}
