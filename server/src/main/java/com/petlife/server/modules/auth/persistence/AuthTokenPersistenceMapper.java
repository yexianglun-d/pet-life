package com.petlife.server.modules.auth.persistence;

import com.petlife.server.modules.auth.persistence.command.CreateUserSessionCommand;
import com.petlife.server.modules.auth.persistence.dataobject.UserSessionDataObject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 认证令牌持久化 Mapper。
 *
 * <p>当前服务端已经切换到数据库作为唯一数据源。登录态基于 `user_sessions` 表持久化，
 * 业务层只关心令牌签发、校验和吊销，不直接感知底层表结构。</p>
 */
@Mapper
public interface AuthTokenPersistenceMapper {

    @Insert("""
        INSERT INTO user_sessions (
          user_id, refresh_token_hash, expires_at, last_active_at, created_at, updated_at
        ) VALUES (
          #{userId}, #{refreshTokenHash}, #{expiresAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserSession(CreateUserSessionCommand command);

    @Select("""
        SELECT user_id
        FROM user_sessions
        WHERE id = #{sessionId}
          AND refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
          AND expires_at > CURRENT_TIMESTAMP
        LIMIT 1
        """)
    Long findActiveUserIdBySession(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Select("""
        SELECT id AS sessionId,
               user_id AS userId
        FROM user_sessions
        WHERE refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
          AND expires_at > CURRENT_TIMESTAMP
        LIMIT 1
        FOR UPDATE
        """)
    UserSessionDataObject lockActiveSessionByRefreshTokenHash(
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE user_sessions
        SET last_active_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int markSessionActive(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE user_sessions
        SET revoked_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{sessionId}
          AND refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int revokeSession(
        @Param("sessionId") Long sessionId,
        @Param("refreshTokenHash") String refreshTokenHash
    );

    @Update("""
        UPDATE user_sessions
        SET revoked_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE refresh_token_hash = #{refreshTokenHash}
          AND revoked_at IS NULL
        """)
    int revokeSessionByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);
}
