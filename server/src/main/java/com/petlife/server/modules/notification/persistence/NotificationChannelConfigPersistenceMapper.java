package com.petlife.server.modules.notification.persistence;

import com.petlife.server.modules.notification.persistence.command.UpdateNotificationChannelConfigStatusCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertNotificationChannelConfigCommand;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationChannelConfigDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通知渠道配置持久化接口。
 */
@Mapper
public interface NotificationChannelConfigPersistenceMapper {

    @Select("""
        SELECT
          id AS channelConfigId,
          channel_type AS channelType,
          provider_code AS providerCode,
          provider_name AS providerName,
          enabled AS enabled,
          config_status AS configStatus,
          remark AS remark,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM notification_channel_configs
        WHERE deleted_at IS NULL
          AND (#{channelType} IS NULL OR channel_type = #{channelType})
          AND (#{providerCode} IS NULL OR provider_code = #{providerCode})
          AND (#{enabled} IS NULL OR enabled = #{enabled})
          AND (#{configStatus} IS NULL OR config_status = #{configStatus})
        ORDER BY updated_at DESC, id DESC
        LIMIT 200
        """)
    List<NotificationChannelConfigDataObject> listChannelConfigs(
        @Param("channelType") String channelType,
        @Param("providerCode") String providerCode,
        @Param("enabled") Boolean enabled,
        @Param("configStatus") String configStatus
    );

    @Select("""
        SELECT
          id AS channelConfigId,
          channel_type AS channelType,
          provider_code AS providerCode,
          provider_name AS providerName,
          enabled AS enabled,
          config_status AS configStatus,
          remark AS remark,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM notification_channel_configs
        WHERE id = #{channelConfigId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    NotificationChannelConfigDataObject findChannelConfigById(@Param("channelConfigId") Long channelConfigId);

    @Select("""
        SELECT
          id AS channelConfigId,
          channel_type AS channelType,
          provider_code AS providerCode,
          provider_name AS providerName,
          enabled AS enabled,
          config_status AS configStatus,
          remark AS remark,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM notification_channel_configs
        WHERE channel_type = #{channelType}
          AND provider_code = #{providerCode}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    NotificationChannelConfigDataObject findChannelConfigByChannelAndProvider(
        @Param("channelType") String channelType,
        @Param("providerCode") String providerCode
    );

    @Insert("""
        INSERT INTO notification_channel_configs (
          channel_type, provider_code, provider_name, enabled,
          config_status, remark, created_at, updated_at
        ) VALUES (
          #{channelType}, #{providerCode}, #{providerName}, #{enabled},
          #{configStatus}, #{remark}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "channelConfigId")
    int insertChannelConfig(UpsertNotificationChannelConfigCommand command);

    @Update("""
        UPDATE notification_channel_configs
        SET channel_type = #{channelType},
            provider_code = #{providerCode},
            provider_name = #{providerName},
            enabled = #{enabled},
            config_status = #{configStatus},
            remark = #{remark},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{channelConfigId}
          AND deleted_at IS NULL
        """)
    int updateChannelConfig(UpsertNotificationChannelConfigCommand command);

    @Update("""
        UPDATE notification_channel_configs
        SET enabled = #{enabled},
            config_status = #{configStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{channelConfigId}
          AND deleted_at IS NULL
        """)
    int updateChannelConfigStatus(UpdateNotificationChannelConfigStatusCommand command);
}
