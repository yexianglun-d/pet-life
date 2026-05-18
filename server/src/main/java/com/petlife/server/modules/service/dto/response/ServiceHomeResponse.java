package com.petlife.server.modules.service.dto.response;

import java.util.List;

/**
 * 服务中心首页聚合响应。
 *
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param opened 当前城市是否已开通服务
 * @param unavailableReason 未开通原因
 * @param categories 服务分类
 * @param featuredProviders 推荐服务商
 * @param upcomingAppointments 即将到来的预约
 * @param commercePlaceholder 商城预留说明
 */
public record ServiceHomeResponse(
    String cityCode,
    String cityName,
    boolean opened,
    String unavailableReason,
    List<ServiceCategoryResponse> categories,
    List<ServiceProviderResponse> featuredProviders,
    List<ServiceAppointmentResponse> upcomingAppointments,
    String commercePlaceholder
) {
}
