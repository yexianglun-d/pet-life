package com.petlife.server.modules.notification.persistence;

import com.petlife.server.modules.notification.persistence.command.CreateNotificationCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationReadCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationsReadCommand;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站内通知持久化 Mapper。
 */
@Mapper
public interface NotificationPersistenceMapper {

    @Select("""
        SELECT
          id AS notificationId,
          user_id AS userId,
          notify_type AS notifyType,
          biz_type AS bizType,
          biz_id AS bizId,
          title AS title,
          content AS content,
          read_status AS readStatus,
          sent_at AS sentAt,
          read_at AS readAt
        FROM notifications
        WHERE user_id = #{userId}
          AND (#{notifyType} IS NULL OR notify_type = #{notifyType})
          AND (#{readStatus} IS NULL OR read_status = #{readStatus})
        ORDER BY sent_at DESC, id DESC
        LIMIT 100
        """)
    List<NotificationDataObject> listNotifications(
        @Param("userId") Long userId,
        @Param("notifyType") String notifyType,
        @Param("readStatus") Integer readStatus
    );

    @Select("""
        SELECT
          id AS notificationId,
          user_id AS userId,
          notify_type AS notifyType,
          biz_type AS bizType,
          biz_id AS bizId,
          title AS title,
          content AS content,
          read_status AS readStatus,
          sent_at AS sentAt,
          read_at AS readAt
        FROM notifications
        WHERE id = #{notificationId}
          AND user_id = #{userId}
        LIMIT 1
        """)
    NotificationDataObject findNotificationByUserIdAndId(
        @Param("userId") Long userId,
        @Param("notificationId") Long notificationId
    );

    @Select("""
        SELECT COUNT(1)
        FROM notifications
        WHERE user_id = #{userId}
          AND read_status = 0
          AND (#{notifyType} IS NULL OR notify_type = #{notifyType})
        """)
    int countUnreadNotifications(
        @Param("userId") Long userId,
        @Param("notifyType") String notifyType
    );

    @Insert("""
        INSERT INTO notifications (
          user_id, notify_type, biz_type, biz_id, title, content,
          read_status, sent_at, created_at, updated_at
        )
        SELECT
          #{userId}, #{notifyType}, #{bizType}, #{bizId}, #{title}, #{content},
          0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        FROM DUAL
        WHERE EXISTS (
          SELECT 1
          FROM user_settings us
          WHERE us.user_id = #{userId}
            AND us.notification_switch = 1
        )
          AND NOT EXISTS (
            SELECT 1
            FROM notifications n
            WHERE n.user_id = #{userId}
              AND n.notify_type = #{notifyType}
              AND n.biz_type <=> #{bizType}
              AND n.biz_id <=> #{bizId}
          )
        """)
    int insertNotificationIfAbsent(CreateNotificationCommand command);

    @Update("""
        UPDATE notifications
        SET read_status = 1,
            read_at = COALESCE(read_at, CURRENT_TIMESTAMP),
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId}
          AND id = #{notificationId}
          AND read_status = 0
        """)
    int markNotificationRead(MarkNotificationReadCommand command);

    @Update("""
        UPDATE notifications
        SET read_status = 1,
            read_at = COALESCE(read_at, CURRENT_TIMESTAMP),
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId}
          AND read_status = 0
          AND (#{notifyType} IS NULL OR notify_type = #{notifyType})
        """)
    int markNotificationsRead(MarkNotificationsReadCommand command);

    @Select("""
        SELECT DISTINCT fm.user_id
        FROM pets p
        JOIN family_members fm
          ON fm.family_id = p.family_id
         AND fm.invite_status = 'joined'
        JOIN user_settings us
          ON us.user_id = fm.user_id
         AND us.notification_switch = 1
        LEFT JOIN family_invitations latest_invitation
          ON latest_invitation.id = (
            SELECT fi.id
            FROM family_invitations fi
            WHERE fi.family_id = fm.family_id
              AND fi.invitee_user_id = fm.user_id
              AND fi.status = 'accepted'
            ORDER BY fi.accepted_at DESC, fi.id DESC
            LIMIT 1
          )
        WHERE p.id = #{petId}
          AND p.deleted_at IS NULL
          AND p.status = 'active'
          AND (
            p.owner_user_id = fm.user_id
            OR fm.role = 'owner'
            OR latest_invitation.id IS NULL
            OR EXISTS (
              SELECT 1
              FROM JSON_TABLE(
                COALESCE(latest_invitation.shared_pet_ids, JSON_ARRAY()),
                '$[*]' COLUMNS (shared_pet_id BIGINT PATH '$')
              ) shared_pet_scope
              WHERE shared_pet_scope.shared_pet_id = p.id
            )
          )
        """)
    List<Long> listNotificationRecipientUserIdsByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT pet_name
        FROM pets
        WHERE id = #{petId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    String findPetNameById(@Param("petId") Long petId);
}
