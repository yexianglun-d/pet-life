package com.petlife.server.modules.admin.domain.entity;

import java.time.LocalDateTime;

/**
 * 后台管理员账号实体。
 */
public final class AdminAccountEntity {

    private final Long adminAccountId;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String roleCode;
    private final Integer status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;

    public AdminAccountEntity(
        Long adminAccountId,
        String username,
        String passwordHash,
        String displayName,
        String roleCode,
        Integer status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
    ) {
        this.adminAccountId = adminAccountId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
    }

    public Long getAdminAccountId() {
        return adminAccountId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
