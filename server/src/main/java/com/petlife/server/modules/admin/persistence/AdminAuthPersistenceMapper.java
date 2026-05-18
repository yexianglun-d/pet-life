package com.petlife.server.modules.admin.persistence;

import com.petlife.server.modules.admin.persistence.command.CreateAdminSessionCommand;
import com.petlife.server.modules.admin.persistence.dataobject.AdminAccountDataObject;
import com.petlife.server.modules.admin.persistence.dataobject.AdminSessionDataObject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 后台账号与登录会话持久化 Mapper。
 */
@Mapper
public interface AdminAuthPersistenceMapper {

    @Select("""
        SELECT
          id AS adminAccountId,
          username AS username,
          password_hash AS passwordHash,
          display_name AS displayName,
          role_code AS roleCode,
          status AS status,
          last_login_at AS lastLoginAt,
          created_at AS createdAt
        FROM admin_accounts
        WHERE username = #{username}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    AdminAccountDataObject findAdminAccountByUsername(@Param("username") String username);

    @Select("""
        SELECT
          id AS adminAccountId,
          username AS username,
          password_hash AS passwordHash,
          display_name AS displayName,
          role_code AS roleCode,
          status AS status,
          last_login_at AS lastLoginAt,
          created_at AS createdAt
        FROM admin_accounts
        WHERE id = #{adminAccountId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    AdminAccountDataObject findAdminAccountById(@Param("adminAccountId") Long adminAccountId);

    @Insert("""
        INSERT INTO admin_sessions (
          admin_account_id, refresh_token_hash, expires_at, last_active_at, created_at, updated_at
        ) VALUES (
          #{adminAccountId}, #{refreshTokenHash}, #{expiresAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAdminSession(CreateAdminSessionCommand command);

    @Select("""
        SELECT
          s.id AS sessionId,
          s.admin_account_id AS adminAccountId
        FROM admin_sessions s
        JOIN admin_accounts a
          ON a.id = s.admin_account_id
         AND a.status = 1
         AND a.deleted_at IS NULL
        WHERE s.refresh_token_hash = #{refreshTokenHash}
          AND s.revoked_at IS NULL
          AND s.expires_at > CURRENT_TIMESTAMP
        LIMIT 1
        FOR UPDATE
        """)
    AdminSessionDataObject lockActiveSessionByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);

    @Select("""
        SELECT
          a.id AS adminAccountId,
          a.username AS username,
          a.password_hash AS passwordHash,
          a.display_name AS displayName,
          a.role_code AS roleCode,
          a.status AS status,
          a.last_login_at AS lastLoginAt,
          a.created_at AS createdAt
        FROM admin_sessions s
        JOIN admin_accounts a
          ON a.id = s.admin_account_id
         AND a.status = 1
         AND a.deleted_at IS NULL
        WHERE s.id = #{sessionId}
          AND s.refresh_token_hash = #{refreshTokenHash}
          AND s.revoked_at IS NULL
          AND s.expires_at > CURRENT_TIMESTAMP
        LIMIT 1
        """)
    AdminAccountDataObject findActiveAdminBySession(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE admin_sessions
        SET last_active_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int markAdminSessionActive(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE admin_sessions
        SET revoked_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int revokeAdminSession(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE admin_sessions
        SET revoked_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int revokeAdminSessionByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);

    @Update("""
        UPDATE admin_accounts
        SET last_login_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{adminAccountId}
          AND deleted_at IS NULL
        """)
    int updateAdminLastLoginAt(@Param("adminAccountId") Long adminAccountId);
}
