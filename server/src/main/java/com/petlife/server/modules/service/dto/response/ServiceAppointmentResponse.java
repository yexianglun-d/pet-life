package com.petlife.server.modules.service.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 服务预约响应。
 *
 * @param appointmentId 预约 ID
 * @param petId 宠物 ID
 * @param petName 宠物名称
 * @param providerId 服务商 ID
 * @param providerName 服务商名称
 * @param providerType 服务商类型
 * @param appointmentType 预约类型
 * @param appointmentDate 预约日期
 * @param appointmentSlot 预约时段
 * @param demandDesc 需求说明
 * @param contactName 联系人
 * @param contactMobile 联系电话
 * @param status 预约状态
 * @param remark 备注
 * @param reviewed 是否已评价
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ServiceAppointmentResponse(
    String appointmentId,
    String petId,
    String petName,
    String providerId,
    String providerName,
    String providerType,
    String appointmentType,
    LocalDate appointmentDate,
    String appointmentSlot,
    String demandDesc,
    String contactName,
    String contactMobile,
    String status,
    String remark,
    boolean reviewed,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
