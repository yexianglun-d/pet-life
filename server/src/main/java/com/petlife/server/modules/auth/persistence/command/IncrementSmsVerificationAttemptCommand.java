package com.petlife.server.modules.auth.persistence.command;

/**
 * 增加验证码校验尝试次数命令。
 */
public class IncrementSmsVerificationAttemptCommand {

    private Long verificationId;
    private String status;

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
