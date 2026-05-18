package com.petlife.server.modules.admin.token.impl;

import com.petlife.server.modules.admin.persistence.AdminAuthPersistenceMapper;
import com.petlife.server.modules.admin.persistence.command.CreateAdminSessionCommand;
import com.petlife.server.modules.admin.persistence.dataobject.AdminAccountDataObject;
import com.petlife.server.modules.admin.persistence.dataobject.AdminSessionDataObject;
import com.petlife.server.modules.admin.security.AuthenticatedAdmin;
import com.petlife.server.modules.admin.token.AdminAccessTokenRepository;
import com.petlife.server.modules.admin.token.AdminIssuedLoginTokens;
import com.petlife.server.modules.auth.token.TokenHashing;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * 基于数据库的后台访问令牌实现。
 *
 * <p>后台账号与用户账号分离存储，避免运营后台复用用户端 token 造成权限边界混淆。</p>
 */
@Repository
public class DatabaseAdminAccessTokenRepository implements AdminAccessTokenRepository {

    private static final long ADMIN_SESSION_EXPIRE_DAYS = 14L;
    private static final String TOKEN_PART_SEPARATOR = ".";

    private final AdminAuthPersistenceMapper adminAuthPersistenceMapper;

    public DatabaseAdminAccessTokenRepository(AdminAuthPersistenceMapper adminAuthPersistenceMapper) {
        this.adminAuthPersistenceMapper = adminAuthPersistenceMapper;
    }

    @Override
    public AdminIssuedLoginTokens issueLoginTokens(Long adminAccountId) {
        String rawSessionToken = generateOpaqueToken();
        CreateAdminSessionCommand command = new CreateAdminSessionCommand();
        command.setAdminAccountId(adminAccountId);
        command.setRefreshTokenHash(TokenHashing.sha256Hex(rawSessionToken));
        command.setExpiresAt(LocalDateTime.now().plusDays(ADMIN_SESSION_EXPIRE_DAYS));
        adminAuthPersistenceMapper.insertAdminSession(command);

        return new AdminIssuedLoginTokens(command.getId() + TOKEN_PART_SEPARATOR + rawSessionToken, rawSessionToken);
    }

    @Override
    public Optional<AdminIssuedLoginTokens> refreshLoginTokens(String refreshToken) {
        String refreshTokenHash = parseRefreshTokenHash(refreshToken);
        if (refreshTokenHash == null) {
            return Optional.empty();
        }

        AdminSessionDataObject activeSession =
            adminAuthPersistenceMapper.lockActiveSessionByRefreshTokenHash(refreshTokenHash);
        if (activeSession == null) {
            return Optional.empty();
        }

        adminAuthPersistenceMapper.revokeAdminSession(activeSession.sessionId(), refreshTokenHash);
        return Optional.of(issueLoginTokens(activeSession.adminAccountId()));
    }

    @Override
    public Optional<AuthenticatedAdmin> findAdminByAccessToken(String accessToken) {
        ParsedAdminSessionToken parsedToken = parseAccessToken(accessToken);
        if (parsedToken == null) {
            return Optional.empty();
        }

        AdminAccountDataObject dataObject = adminAuthPersistenceMapper.findActiveAdminBySession(
            parsedToken.sessionId(),
            parsedToken.tokenHash()
        );
        if (dataObject == null) {
            return Optional.empty();
        }

        adminAuthPersistenceMapper.markAdminSessionActive(parsedToken.sessionId(), parsedToken.tokenHash());
        return Optional.of(new AuthenticatedAdmin(
            dataObject.adminAccountId(),
            dataObject.username(),
            dataObject.displayName(),
            dataObject.roleCode()
        ));
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        String refreshTokenHash = parseRefreshTokenHash(refreshToken);
        if (refreshTokenHash != null) {
            adminAuthPersistenceMapper.revokeAdminSessionByRefreshTokenHash(refreshTokenHash);
        }
    }

    private String generateOpaqueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ParsedAdminSessionToken parseAccessToken(String accessToken) {
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
            return new ParsedAdminSessionToken(sessionId, tokenHash);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String parseRefreshTokenHash(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        return TokenHashing.sha256Hex(refreshToken.trim());
    }

    private record ParsedAdminSessionToken(
        Long sessionId,
        String tokenHash
    ) {
    }
}
