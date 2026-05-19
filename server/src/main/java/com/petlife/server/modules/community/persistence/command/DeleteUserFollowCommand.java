package com.petlife.server.modules.community.persistence.command;

/**
 * 取消用户关注命令。
 */
public class DeleteUserFollowCommand {

    private Long followerUserId;
    private Long followedUserId;

    public Long getFollowerUserId() {
        return followerUserId;
    }

    public void setFollowerUserId(Long followerUserId) {
        this.followerUserId = followerUserId;
    }

    public Long getFollowedUserId() {
        return followedUserId;
    }

    public void setFollowedUserId(Long followedUserId) {
        this.followedUserId = followedUserId;
    }
}
