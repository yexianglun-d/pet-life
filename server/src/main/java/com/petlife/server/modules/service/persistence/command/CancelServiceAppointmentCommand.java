package com.petlife.server.modules.service.persistence.command;

/**
 * 取消服务预约持久化命令。
 */
public class CancelServiceAppointmentCommand {

    private Long appointmentId;
    private Long userId;
    private String cancelReason;

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
