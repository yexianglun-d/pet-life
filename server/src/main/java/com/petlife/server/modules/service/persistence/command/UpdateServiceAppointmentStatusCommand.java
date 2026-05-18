package com.petlife.server.modules.service.persistence.command;

/**
 * 后台更新预约状态持久化命令。
 */
public class UpdateServiceAppointmentStatusCommand {

    private Long appointmentId;
    private String status;
    private String remark;

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
