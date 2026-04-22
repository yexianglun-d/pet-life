package com.petlife.server.modules.health.persistence;

import com.petlife.server.modules.health.persistence.command.CreateHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.DeleteHealthRecordCommand;
import com.petlife.server.modules.health.persistence.command.UpdateHealthRecordCommand;
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
          id AS healthRecordId,
          pet_id AS petId,
          operator_user_id AS operatorUserId,
          record_type AS recordType,
          title AS title,
          occurred_at AS occurredAt,
          result_summary AS resultSummary,
          notes AS notes,
          created_at AS createdAt
        FROM pet_health_records
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY occurred_at DESC, id DESC
        """)
    List<HealthRecordDataObject> listHealthRecordsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS healthRecordId,
          pet_id AS petId,
          operator_user_id AS operatorUserId,
          record_type AS recordType,
          title AS title,
          occurred_at AS occurredAt,
          result_summary AS resultSummary,
          notes AS notes,
          created_at AS createdAt
        FROM pet_health_records
        WHERE id = #{healthRecordId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    HealthRecordDataObject findHealthRecordById(@Param("healthRecordId") Long healthRecordId);

    @Select("""
        SELECT
          id AS healthRecordId,
          pet_id AS petId,
          operator_user_id AS operatorUserId,
          record_type AS recordType,
          title AS title,
          occurred_at AS occurredAt,
          result_summary AS resultSummary,
          notes AS notes,
          created_at AS createdAt
        FROM pet_health_records
        WHERE pet_id = #{petId}
          AND id = #{healthRecordId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    HealthRecordDataObject findHealthRecordByPetIdAndId(
        @Param("petId") Long petId,
        @Param("healthRecordId") Long healthRecordId
    );

    @Insert("""
        INSERT INTO pet_health_records (
          pet_id, operator_user_id, record_type, title, occurred_at, result_summary,
          notes, created_at, updated_at
        ) VALUES (
          #{petId}, #{operatorUserId}, #{recordType}, #{title}, #{occurredAt}, #{resultSummary},
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
            result_summary = #{resultSummary},
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
