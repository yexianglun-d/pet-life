package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 服务预约创建请求。
 *
 * @param petId 关联宠物 ID
 * @param providerId 服务商 ID
 * @param appointmentType 预约类型
 * @param appointmentDate 预约日期
 * @param appointmentSlot 预约时段
 * @param demandDesc 需求说明
 * @param contactName 联系人
 * @param contactMobile 联系电话
 */
public record CreateServiceAppointmentRequest(
    @NotNull(message = "宠物 ID 不能为空")
    Long petId,
    @NotNull(message = "服务商 ID 不能为空")
    Long providerId,
    @NotBlank(message = "预约类型不能为空")
    String appointmentType,
    @NotNull(message = "预约日期不能为空")
    LocalDate appointmentDate,
    @NotBlank(message = "预约时段不能为空")
    @Size(max = 50, message = "预约时段长度不能超过 50 个字符")
    String appointmentSlot,
    @Size(max = 500, message = "需求说明长度不能超过 500 个字符")
    String demandDesc,
    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人长度不能超过 50 个字符")
    String contactName,
    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    String contactMobile
) {
}
