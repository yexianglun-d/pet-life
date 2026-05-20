package com.petlife.server.modules.notification.persistence;

import com.petlife.server.modules.notification.persistence.command.CreatePushDeliveryRecordCommand;
import com.petlife.server.modules.notification.persistence.command.CreatePushTaskCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertPushDeviceTokenCommand;
import com.petlife.server.modules.notification.persistence.dataobject.PushDeliveryRecordDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.PushDeviceTokenDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.PushTaskDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Push 通知持久化 Mapper。
 */
@Mapper
public interface PushNotificationPersistenceMapper {

    String DEVICE_TOKEN_SELECT_COLUMNS = """
          id AS deviceTokenId,
          user_id AS userId,
          platform AS platform,
          provider_code AS providerCode,
          RIGHT(device_token, 6) AS deviceTokenSuffix,
          device_id AS deviceId,
          app_version AS appVersion,
          enabled AS enabled,
          last_registered_at AS lastRegisteredAt,
          unregistered_at AS unregisteredAt,
          created_at AS createdAt,
          updated_at AS updatedAt
        """;

    String PUSH_TASK_SELECT_COLUMNS = """
          id AS pushTaskId,
          user_id AS userId,
          notification_id AS notificationId,
          notify_type AS notifyType,
          biz_type AS bizType,
          biz_id AS bizId,
          title AS title,
          content AS content,
          provider_code AS providerCode,
          task_status AS taskStatus,
          failure_reason AS failureReason,
          created_at AS createdAt,
          updated_at AS updatedAt
        """;

    String DELIVERY_SELECT_COLUMNS = """
          id AS deliveryRecordId,
          push_task_id AS pushTaskId,
          device_token_id AS deviceTokenId,
          user_id AS userId,
          provider_code AS providerCode,
          delivery_status AS deliveryStatus,
          failure_reason AS failureReason,
          attempted_at AS attemptedAt,
          created_at AS createdAt
        """;

    @Insert("""
        INSERT INTO user_push_device_tokens (
          user_id, platform, provider_code, device_token, device_id, app_version,
          enabled, last_registered_at, unregistered_at, created_at, updated_at
        ) VALUES (
          #{userId}, #{platform}, #{providerCode}, #{deviceToken}, #{deviceId}, #{appVersion},
          1, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE
          id = LAST_INSERT_ID(id),
          user_id = VALUES(user_id),
          platform = VALUES(platform),
          device_id = VALUES(device_id),
          app_version = VALUES(app_version),
          enabled = 1,
          last_registered_at = CURRENT_TIMESTAMP,
          unregistered_at = NULL,
          updated_at = CURRENT_TIMESTAMP
        """)
    @Options(useGeneratedKeys = true, keyProperty = "deviceTokenId")
    int upsertDeviceToken(UpsertPushDeviceTokenCommand command);

    @Select("""
        SELECT
        """ + DEVICE_TOKEN_SELECT_COLUMNS + """
        FROM user_push_device_tokens
        WHERE id = #{deviceTokenId}
        LIMIT 1
        """)
    PushDeviceTokenDataObject findDeviceTokenById(@Param("deviceTokenId") Long deviceTokenId);

    @Select("""
        SELECT
        """ + DEVICE_TOKEN_SELECT_COLUMNS + """
        FROM user_push_device_tokens
        WHERE id = #{deviceTokenId}
          AND user_id = #{userId}
        LIMIT 1
        """)
    PushDeviceTokenDataObject findUserDeviceTokenById(
        @Param("userId") Long userId,
        @Param("deviceTokenId") Long deviceTokenId
    );

    @Select("""
        SELECT
        """ + DEVICE_TOKEN_SELECT_COLUMNS + """
        FROM user_push_device_tokens
        WHERE user_id = #{userId}
          AND enabled = 1
        ORDER BY last_registered_at DESC, id DESC
        """)
    List<PushDeviceTokenDataObject> listActiveDeviceTokensByUserId(@Param("userId") Long userId);

    @Update("""
        UPDATE user_push_device_tokens
        SET enabled = 0,
            unregistered_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{deviceTokenId}
          AND user_id = #{userId}
          AND enabled = 1
        """)
    int disableUserDeviceToken(
        @Param("userId") Long userId,
        @Param("deviceTokenId") Long deviceTokenId
    );

    @Insert("""
        INSERT INTO push_tasks (
          user_id, notification_id, notify_type, biz_type, biz_id, title, content,
          provider_code, task_status, failure_reason, created_at, updated_at
        ) VALUES (
          #{userId}, #{notificationId}, #{notifyType}, #{bizType}, #{bizId}, #{title}, #{content},
          #{providerCode}, #{taskStatus}, #{failureReason}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "pushTaskId")
    int insertPushTask(CreatePushTaskCommand command);

    @Insert("""
        INSERT INTO push_delivery_records (
          push_task_id, device_token_id, user_id, provider_code,
          delivery_status, failure_reason, attempted_at, created_at
        ) VALUES (
          #{pushTaskId}, #{deviceTokenId}, #{userId}, #{providerCode},
          #{deliveryStatus}, #{failureReason}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    int insertDeliveryRecord(CreatePushDeliveryRecordCommand command);

    @Select("""
        SELECT COUNT(1)
        FROM push_tasks
        WHERE user_id = #{userId}
          AND biz_type <=> #{bizType}
          AND biz_id <=> #{bizId}
          AND failure_reason = #{failureReason}
        """)
    int countPushTasksByBusinessAndReason(
        @Param("userId") Long userId,
        @Param("bizType") String bizType,
        @Param("bizId") Long bizId,
        @Param("failureReason") String failureReason
    );

    @Select("""
        SELECT
        """ + PUSH_TASK_SELECT_COLUMNS + """
        FROM push_tasks
        WHERE (#{userId} IS NULL OR user_id = #{userId})
          AND (#{notificationId} IS NULL OR notification_id = #{notificationId})
          AND (#{taskStatus} IS NULL OR task_status = #{taskStatus})
          AND (#{providerCode} IS NULL OR provider_code = #{providerCode})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<PushTaskDataObject> listPushTasks(
        @Param("userId") Long userId,
        @Param("notificationId") Long notificationId,
        @Param("taskStatus") String taskStatus,
        @Param("providerCode") String providerCode
    );

    @Select("""
        SELECT
        """ + DELIVERY_SELECT_COLUMNS + """
        FROM push_delivery_records
        WHERE (#{pushTaskId} IS NULL OR push_task_id = #{pushTaskId})
          AND (#{userId} IS NULL OR user_id = #{userId})
          AND (#{deliveryStatus} IS NULL OR delivery_status = #{deliveryStatus})
          AND (#{providerCode} IS NULL OR provider_code = #{providerCode})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<PushDeliveryRecordDataObject> listDeliveryRecords(
        @Param("pushTaskId") Long pushTaskId,
        @Param("userId") Long userId,
        @Param("deliveryStatus") String deliveryStatus,
        @Param("providerCode") String providerCode
    );
}
