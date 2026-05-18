package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台更新预约状态请求。
 */
public record AdminUpdateServiceAppointmentStatusRequest(
    @NotBlank(message = "预约状态不能为空")
    String status,
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    String remark
) {
}
