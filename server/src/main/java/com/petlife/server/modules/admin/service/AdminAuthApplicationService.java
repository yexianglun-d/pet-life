package com.petlife.server.modules.admin.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.converter.AdminAccountConverter;
import com.petlife.server.modules.admin.domain.entity.AdminAccountEntity;
import com.petlife.server.modules.admin.dto.request.AdminLoginRequest;
import com.petlife.server.modules.admin.dto.request.AdminLogoutRequest;
import com.petlife.server.modules.admin.dto.request.AdminRefreshTokenRequest;
import com.petlife.server.modules.admin.dto.response.AdminLoginResponse;
import com.petlife.server.modules.admin.dto.response.AdminRefreshTokenResponse;
import com.petlife.server.modules.admin.persistence.AdminAuthPersistenceMapper;
import com.petlife.server.modules.admin.token.AdminAccessTokenRepository;
import com.petlife.server.modules.admin.token.AdminIssuedLoginTokens;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台认证应用服务。
 */
@Service
public class AdminAuthApplicationService {

    private static final Integer ADMIN_ACCOUNT_STATUS_ACTIVE = 1;

    private final AdminAuthPersistenceMapper adminAuthPersistenceMapper;
    private final AdminAccessTokenRepository adminAccessTokenRepository;
    private final AdminAccountConverter adminAccountConverter;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthApplicationService(
        AdminAuthPersistenceMapper adminAuthPersistenceMapper,
        AdminAccessTokenRepository adminAccessTokenRepository,
        AdminAccountConverter adminAccountConverter,
        PasswordEncoder passwordEncoder
    ) {
        this.adminAuthPersistenceMapper = adminAuthPersistenceMapper;
        this.adminAccessTokenRepository = adminAccessTokenRepository;
        this.adminAccountConverter = adminAccountConverter;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminAccountEntity adminAccount = adminAccountConverter.toEntity(
            adminAuthPersistenceMapper.findAdminAccountByUsername(request.username().trim())
        );
        if (adminAccount == null
            || !ADMIN_ACCOUNT_STATUS_ACTIVE.equals(adminAccount.getStatus())
            || adminAccount.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), adminAccount.getPasswordHash())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "后台账号或密码错误");
        }

        adminAuthPersistenceMapper.updateAdminLastLoginAt(adminAccount.getAdminAccountId());
        AdminAccountEntity refreshedAdminAccount = adminAccountConverter.toEntity(
            adminAuthPersistenceMapper.findAdminAccountById(adminAccount.getAdminAccountId())
        );
        AdminIssuedLoginTokens issuedLoginTokens =
            adminAccessTokenRepository.issueLoginTokens(adminAccount.getAdminAccountId());
        return new AdminLoginResponse(
            issuedLoginTokens.accessToken(),
            issuedLoginTokens.refreshToken(),
            adminAccountConverter.toResponse(refreshedAdminAccount)
        );
    }

    @Transactional
    public AdminRefreshTokenResponse refreshToken(AdminRefreshTokenRequest request) {
        AdminIssuedLoginTokens issuedLoginTokens = adminAccessTokenRepository.refreshLoginTokens(request.refreshToken())
            .orElseThrow(() -> new BusinessException(ResponseCode.AUTH_REFRESH_TOKEN_INVALID, "后台登录状态已失效"));
        return new AdminRefreshTokenResponse(issuedLoginTokens.accessToken(), issuedLoginTokens.refreshToken());
    }

    @Transactional
    public void logout(AdminLogoutRequest request) {
        adminAccessTokenRepository.revokeRefreshToken(request.refreshToken());
    }
}
