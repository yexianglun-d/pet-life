package com.petlife.server.modules.service.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.dto.response.AuditLogResponse;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.service.dto.request.AdminUpdateProviderReviewStatusRequest;
import com.petlife.server.modules.service.dto.request.AdminUpdateServiceAppointmentStatusRequest;
import com.petlife.server.modules.service.dto.request.AdminUpdateProviderLocationRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertProviderScheduleSlotRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertProviderServiceItemRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertServiceCityConfigRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertServiceProviderRequest;
import com.petlife.server.modules.service.dto.response.ProviderReviewResponse;
import com.petlife.server.modules.service.dto.response.ServiceAppointmentResponse;
import com.petlife.server.modules.service.dto.response.ServiceCityConfigResponse;
import com.petlife.server.modules.service.dto.response.ServiceProviderResponse;
import com.petlife.server.modules.service.service.ServiceCenterApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台服务中心控制器。
 *
 * <p>当前开放服务城市、服务商、服务项目、预约时段、预约状态和评价治理；
 * 商城履约与设备厂商接入保持预留。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/service")
public class AdminServiceCenterController {

    private final ServiceCenterApplicationService serviceCenterApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminServiceCenterController(
        ServiceCenterApplicationService serviceCenterApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.serviceCenterApplicationService = serviceCenterApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping("/cities")
    public ApiResponse<List<ServiceCityConfigResponse>> listCities(
        @RequestParam(value = "city_code", required = false) String cityCode,
        @RequestParam(value = "opened", required = false) Boolean opened
    ) {
        return ApiResponse.success(serviceCenterApplicationService.listAdminCityConfigs(cityCode, opened));
    }

    @PostMapping("/cities")
    public ApiResponse<ServiceCityConfigResponse> upsertCity(
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertServiceCityConfigRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.upsertAdminCityConfig(
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @GetMapping("/providers")
    public ApiResponse<List<ServiceProviderResponse>> listProviders(
        @RequestParam(value = "provider_type", required = false) String providerType,
        @RequestParam(value = "city_code", required = false) String cityCode,
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.listAdminProviders(providerType, cityCode, status)
        );
    }

    @PostMapping("/providers")
    public ApiResponse<ServiceProviderResponse> createProvider(
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertServiceProviderRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.createAdminProvider(
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/providers/{providerId}")
    public ApiResponse<ServiceProviderResponse> updateProvider(
        @PathVariable Long providerId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertServiceProviderRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminProvider(
                providerId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/providers/{providerId}/location")
    public ApiResponse<ServiceProviderResponse> updateProviderLocation(
        @PathVariable Long providerId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateProviderLocationRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminProviderLocation(
                providerId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PostMapping("/providers/{providerId}/items")
    public ApiResponse<ServiceProviderResponse> createServiceItem(
        @PathVariable Long providerId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertProviderServiceItemRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.createAdminServiceItem(
                providerId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/providers/{providerId}/items/{serviceItemId}")
    public ApiResponse<ServiceProviderResponse> updateServiceItem(
        @PathVariable Long providerId,
        @PathVariable Long serviceItemId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertProviderServiceItemRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminServiceItem(
                providerId,
                serviceItemId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PostMapping("/providers/{providerId}/slots")
    public ApiResponse<ServiceProviderResponse> createScheduleSlot(
        @PathVariable Long providerId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertProviderScheduleSlotRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.createAdminScheduleSlot(
                providerId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/providers/{providerId}/slots/{slotId}")
    public ApiResponse<ServiceProviderResponse> updateScheduleSlot(
        @PathVariable Long providerId,
        @PathVariable Long slotId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpsertProviderScheduleSlotRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminScheduleSlot(
                providerId,
                slotId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @GetMapping("/appointments")
    public ApiResponse<List<ServiceAppointmentResponse>> listAppointments(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "provider_type", required = false) String providerType,
        @RequestParam(value = "city_code", required = false) String cityCode
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.listAdminAppointments(status, providerType, cityCode)
        );
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ProviderReviewResponse>> listReviews(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "provider_type", required = false) String providerType,
        @RequestParam(value = "city_code", required = false) String cityCode
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.listAdminReviews(status, providerType, cityCode)
        );
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    public ApiResponse<ServiceAppointmentResponse> updateAppointmentStatus(
        @PathVariable Long appointmentId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateServiceAppointmentStatusRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminAppointmentStatus(
                appointmentId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @PatchMapping("/reviews/{reviewId}/status")
    public ApiResponse<ProviderReviewResponse> updateReviewStatus(
        @PathVariable Long reviewId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateProviderReviewStatusRequest request
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.updateAdminReviewStatus(
                reviewId,
                request,
                auditContext(operatorName, httpServletRequest)
            )
        );
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLogResponse>> listAuditLogs(
        @RequestParam(value = "operator_id", required = false) String operatorId,
        @RequestParam(value = "target_type", required = false) String targetType,
        @RequestParam(value = "action", required = false) String action
    ) {
        return ApiResponse.success(auditLogApplicationService.listServiceAuditLogs(operatorId, targetType, action));
    }

    private AdminOperationContext auditContext(String operatorName, HttpServletRequest httpServletRequest) {
        return auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
    }
}
