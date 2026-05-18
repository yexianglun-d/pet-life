package com.petlife.server.modules.admin.token;

import com.petlife.server.modules.admin.security.AuthenticatedAdmin;
import java.util.Optional;

/**
 * 后台访问令牌存储接口。
 */
public interface AdminAccessTokenRepository {

    AdminIssuedLoginTokens issueLoginTokens(Long adminAccountId);

    Optional<AdminIssuedLoginTokens> refreshLoginTokens(String refreshToken);

    Optional<AuthenticatedAdmin> findAdminByAccessToken(String accessToken);

    void revokeRefreshToken(String refreshToken);
}
