package com.petlife.server.modules.user.persistence.command;

/**
 * 用户资料更新命令。
 */
public class UpdateUserProfileCommand {

    private Long userId;
    private String nickname;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
