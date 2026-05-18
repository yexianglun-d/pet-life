package com.petlife.server.modules.admin.domain.entity;

/**
 * 后台用户上下文实体。
 */
public final class AdminUserContextEntity {

    private final Long userId;
    private final String nickname;
    private final String mobile;

    public AdminUserContextEntity(Long userId, String nickname, String mobile) {
        this.userId = userId;
        this.nickname = nickname;
        this.mobile = mobile;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getMobile() {
        return mobile;
    }
}
