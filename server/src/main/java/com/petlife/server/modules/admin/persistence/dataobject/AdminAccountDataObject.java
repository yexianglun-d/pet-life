package com.petlife.server.modules.admin.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 后台管理员账号数据对象。
 */
public record AdminAccountDataObject(
    Long adminAccountId,
    String username,
    String passwordHash,
    String displayName,
    String roleCode,
    Integer status,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
}
