package com.petlife.server.modules.auth.persistence.dataobject;

/**
 * 用户会话读模型。
 */
public class UserSessionDataObject {

    private Long sessionId;
    private Long userId;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
