package com.petlife.server.modules.community.domain.entity;

/**
 * 社区帖子作者实体。
 */
public final class CommunityAuthorEntity {

    private final Long userId;
    private final String nickname;
    private final String avatarUrl;

    public CommunityAuthorEntity(Long userId, String nickname, String avatarUrl) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
