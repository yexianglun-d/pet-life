package com.petlife.server.modules.auth.token.impl;

import com.petlife.server.modules.auth.persistence.command.CreateUserSessionCommand;
import com.petlife.server.modules.auth.persistence.AuthTokenPersistenceMapper;
import com.petlife.server.modules.auth.token.AccessTokenRepository;
import com.petlife.server.modules.auth.token.IssuedLoginTokens;
import com.petlife.server.modules.auth.token.TokenHashing;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * 数据库访问令牌实现。
 *
 * <p>数据库只保存 token 摘要，避免数据库泄漏时直接暴露可用登录凭证。</p>
 */
@Repository
public class DatabaseAccessTokenRepository implements AccessTokenRepository {

    private static final long LOGIN_SESSION_EXPIRE_DAYS = 30L;
    private static final String TOKEN_PART_SEPARATOR = ".";

    private final AuthTokenPersistenceMapper authTokenPersistenceMapper;

    public DatabaseAccessTokenRepository(AuthTokenPersistenceMapper authTokenPersistenceMapper) {
        this.authTokenPersistenceMapper = authTokenPersistenceMapper;
    }

    @Override
    public IssuedLoginTokens issueLoginTokens(Long userId) {
        String rawSessionToken = generateOpaqueToken();
        CreateUserSessionCommand command = new CreateUserSessionCommand();
        command.setUserId(userId);
        command.setRefreshTokenHash(TokenHashing.sha256Hex(rawSessionToken));
        command.setExpiresAt(LocalDateTime.now().plusDays(LOGIN_SESSION_EXPIRE_DAYS));
        authTokenPersistenceMapper.insertUserSession(command);

        String accessToken = command.getId() + TOKEN_PART_SEPARATOR + rawSessionToken;
        return new IssuedLoginTokens(accessToken, rawSessionToken);
    }

    @Override
    public Optional<Long> findUserIdByAccessToken(String accessToken) {
        ParsedSessionToken parsedToken = parseAccessToken(accessToken);
        if (parsedToken == null) {
            return Optional.empty();
        }

        Long userId = authTokenPersistenceMapper.findActiveUserIdBySession(
            parsedToken.sessionId(),
            parsedToken.tokenHash()
        );
        if (userId != null) {
            authTokenPersistenceMapper.markSessionActive(parsedToken.sessionId(), parsedToken.tokenHash());
        }
        return Optional.ofNullable(userId);
    }

    @Override
    public void revokeAccessToken(String accessToken) {
        ParsedSessionToken parsedToken = parseAccessToken(accessToken);
        if (parsedToken != null) {
            authTokenPersistenceMapper.revokeSession(parsedToken.sessionId(), parsedToken.tokenHash());
        }
    }

    private String generateOpaqueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ParsedSessionToken parseAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank() || !accessToken.contains(TOKEN_PART_SEPARATOR)) {
            return null;
        }

        String[] tokenParts = accessToken.split("\\.", 2);
        try {
            Long sessionId = Long.valueOf(tokenParts[0]);
            String tokenHash = TokenHashing.sha256Hex(tokenParts[1]);
            if (tokenHash == null) {
                return null;
            }
            return new ParsedSessionToken(sessionId, tokenHash);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record ParsedSessionToken(
        Long sessionId,
        String tokenHash
    ) {
    }
}
