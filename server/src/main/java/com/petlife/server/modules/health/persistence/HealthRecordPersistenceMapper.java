package com.petlife.server.modules.health.persistence;

import com.petlife.server.modules.health.persistence.record.HealthRecordPersistenceRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
          notes AS notes,
          created_at AS createdAt
        FROM pet_health_records
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY occurred_at DESC, id DESC
        """)
    List<HealthRecordPersistenceRecord> listHealthRecordsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT
          id AS healthRecordId,
          pet_id AS petId,
          operator_user_id AS operatorUserId,
          record_type AS recordType,
          title AS title,
          occurred_at AS occurredAt,
          notes AS notes,
          created_at AS createdAt
        FROM pet_health_records
        WHERE id = #{healthRecordId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    HealthRecordPersistenceRecord findHealthRecordById(@Param("healthRecordId") Long healthRecordId);

    @Insert("""
        INSERT INTO pet_health_records (
          pet_id, operator_user_id, record_type, title, occurred_at, result_summary,
          notes, created_at, updated_at
        ) VALUES (
          #{petId}, #{operatorUserId}, #{recordType}, #{title}, #{occurredAt}, #{resultSummary},
          #{notes}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    int insertHealthRecord(
        @Param("petId") Long petId,
        @Param("operatorUserId") Long operatorUserId,
        @Param("recordType") String recordType,
        @Param("title") String title,
        @Param("occurredAt") LocalDateTime occurredAt,
        @Param("resultSummary") String resultSummary,
        @Param("notes") String notes
    );

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertId();
}
