package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 服务预约取消请求。
 *
 * @param cancelReason 取消原因
 */
public record CancelServiceAppointmentRequest(
    @Size(max = 500, message = "取消原因长度不能超过 500 个字符")
    String cancelReason
) {
}
