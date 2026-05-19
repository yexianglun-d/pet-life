package com.petlife.server.modules.auth.persistence.command;

/**
 * 更新验证码状态命令。
 */
public class UpdateSmsVerificationStatusCommand {

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
