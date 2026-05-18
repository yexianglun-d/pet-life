package com.petlife.server.modules.service.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 服务商可预约时段响应。
 *
 * @param slotId 时段 ID
 * @param providerId 服务商 ID
 * @param appointmentType 预约类型
 * @param slotDate 预约日期
 * @param startTime 开始时间
 * @param endTime 结束时间
 * @param quota 总名额
 * @param bookedCount 已预约数量
 * @param availableQuota 可预约名额
 * @param status 时段状态
 * @param bookable 是否可预约
 */
public record ProviderScheduleSlotResponse(
    String slotId,
    String providerId,
    String appointmentType,
    LocalDate slotDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer quota,
    Integer bookedCount,
    Integer availableQuota,
    String status,
    boolean bookable
) {
}
