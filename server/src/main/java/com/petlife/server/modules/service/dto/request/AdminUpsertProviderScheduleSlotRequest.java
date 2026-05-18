package com.petlife.server.modules.service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 后台新增或编辑预约时段请求。
 */
public record AdminUpsertProviderScheduleSlotRequest(
    @NotBlank(message = "预约类型不能为空")
    String appointmentType,
    @NotNull(message = "预约日期不能为空")
    LocalDate slotDate,
    @NotNull(message = "开始时间不能为空")
    LocalTime startTime,
    @NotNull(message = "结束时间不能为空")
    LocalTime endTime,
    @NotNull(message = "可预约名额不能为空")
    @Min(value = 0, message = "可预约名额不能小于 0")
    Integer quota,
    @NotBlank(message = "时段状态不能为空")
    String status
) {
}
