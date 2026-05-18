package com.petlife.server.modules.service.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 服务商响应。
 *
 * @param providerId 服务商 ID
 * @param providerType 服务商类型
 * @param providerName 服务商名称
 * @param cityCode 城市编码
 * @param address 地址
 * @param latitude 纬度
 * @param longitude 经度
 * @param contactPhone 联系电话
 * @param businessHours 营业时间
 * @param ratingAvg 平均评分
 * @param reviewCount 评价数量
 * @param status 状态
 * @param bookable 是否可预约
 * @param serviceItems 服务项目
 * @param availableSlots 可预约时段
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ServiceProviderResponse(
    String providerId,
    String providerType,
    String providerName,
    String cityCode,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String contactPhone,
    String businessHours,
    BigDecimal ratingAvg,
    Integer reviewCount,
    String status,
    boolean bookable,
    List<ProviderServiceItemResponse> serviceItems,
    List<ProviderScheduleSlotResponse> availableSlots,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
