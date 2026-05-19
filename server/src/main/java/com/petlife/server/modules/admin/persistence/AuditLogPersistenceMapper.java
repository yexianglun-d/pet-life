package com.petlife.server.modules.admin.persistence;

import com.petlife.server.modules.admin.persistence.command.CreateAuditLogCommand;
import com.petlife.server.modules.admin.persistence.dataobject.AuditLogDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 后台审计日志持久化 Mapper。
 */
@Mapper
public interface AuditLogPersistenceMapper {

    @Insert("""
        INSERT INTO audit_logs (
          operator_type, operator_id, target_type, target_id, action,
          detail_json, ip_address, user_agent, created_at
        ) VALUES (
          #{operatorType}, #{operatorId}, #{targetType}, #{targetId}, #{action},
          #{detailJson}, #{ipAddress}, #{userAgent}, CURRENT_TIMESTAMP
        )
        """)
    int insertAuditLog(CreateAuditLogCommand command);

    @Select("""
        SELECT
          id AS auditLogId,
          operator_type AS operatorType,
          operator_id AS operatorId,
          target_type AS targetType,
          target_id AS targetId,
          action AS action,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(detail_json, JSON_OBJECT()), '$')) AS detailJson,
          ip_address AS ipAddress,
          user_agent AS userAgent,
          created_at AS createdAt
        FROM audit_logs
        WHERE target_type IN (
          'service_city',
          'service_provider',
          'provider_service_item',
          'provider_schedule_slot',
          'service_appointment',
          'provider_review'
        )
          AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
          AND (#{targetType} IS NULL OR target_type = #{targetType})
          AND (#{action} IS NULL OR action = #{action})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<AuditLogDataObject> listServiceAuditLogs(
        @Param("operatorId") String operatorId,
        @Param("targetType") String targetType,
        @Param("action") String action
    );

    @Select("""
        SELECT
          id AS auditLogId,
          operator_type AS operatorType,
          operator_id AS operatorId,
          target_type AS targetType,
          target_id AS targetId,
          action AS action,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(detail_json, JSON_OBJECT()), '$')) AS detailJson,
          ip_address AS ipAddress,
          user_agent AS userAgent,
          created_at AS createdAt
        FROM audit_logs
        WHERE target_type IN ('moderation_report', 'community_post', 'community_question')
          AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
          AND (#{targetType} IS NULL OR target_type = #{targetType})
          AND (#{action} IS NULL OR action = #{action})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<AuditLogDataObject> listModerationAuditLogs(
        @Param("operatorId") String operatorId,
        @Param("targetType") String targetType,
        @Param("action") String action
    );
}
