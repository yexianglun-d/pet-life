package com.petlife.server.modules.service.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.service.dto.request.CancelServiceAppointmentRequest;
import com.petlife.server.modules.service.dto.request.CreateProviderReviewRequest;
import com.petlife.server.modules.service.dto.request.CreateServiceAppointmentRequest;
import com.petlife.server.modules.service.dto.response.ProviderScheduleSlotResponse;
import com.petlife.server.modules.service.dto.response.ProviderReviewResponse;
import com.petlife.server.modules.service.dto.response.ServiceAppointmentResponse;
import com.petlife.server.modules.service.dto.response.ServiceHomeResponse;
import com.petlife.server.modules.service.dto.response.ServiceProviderResponse;
import com.petlife.server.modules.service.service.ServiceCenterApplicationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务中心控制器。
 */
@RestController
@RequestMapping("/api/v1")
public class ServiceCenterController {

    private final ServiceCenterApplicationService serviceCenterApplicationService;

    public ServiceCenterController(ServiceCenterApplicationService serviceCenterApplicationService) {
        this.serviceCenterApplicationService = serviceCenterApplicationService;
    }

    @GetMapping("/services/home")
    public ApiResponse<ServiceHomeResponse> getServiceHome(
        @RequestParam(value = "pet_id", required = false) Long petId,
        @RequestParam(value = "city_code", required = false) String cityCode
    ) {
        return ApiResponse.success(serviceCenterApplicationService.getServiceHome(petId, cityCode));
    }

    @GetMapping("/providers")
    public ApiResponse<List<ServiceProviderResponse>> listProviders(
        @RequestParam(value = "provider_type", required = false) String providerType,
        @RequestParam(value = "city_code", required = false) String cityCode
    ) {
        return ApiResponse.success(serviceCenterApplicationService.listProviders(providerType, cityCode));
    }

    @GetMapping("/providers/{providerId}")
    public ApiResponse<ServiceProviderResponse> getProviderDetail(@PathVariable Long providerId) {
        return ApiResponse.success(serviceCenterApplicationService.getProviderDetail(providerId));
    }

    @GetMapping("/providers/{providerId}/slots")
    public ApiResponse<List<ProviderScheduleSlotResponse>> listProviderSlots(
        @PathVariable Long providerId,
        @RequestParam(value = "appointment_type", required = false) String appointmentType,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(value = "start_date", required = false) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(value = "end_date", required = false) LocalDate endDate
    ) {
        return ApiResponse.success(
            serviceCenterApplicationService.listProviderSlots(providerId, appointmentType, startDate, endDate)
        );
    }

    @GetMapping("/providers/{providerId}/reviews")
    public ApiResponse<List<ProviderReviewResponse>> listProviderReviews(@PathVariable Long providerId) {
        return ApiResponse.success(serviceCenterApplicationService.listProviderReviews(providerId));
    }

    @GetMapping("/appointments")
    public ApiResponse<List<ServiceAppointmentResponse>> listAppointments(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(serviceCenterApplicationService.listAppointments(status));
    }

    @PostMapping("/appointments")
    public ApiResponse<ServiceAppointmentResponse> createAppointment(
        @Valid @RequestBody CreateServiceAppointmentRequest request
    ) {
        return ApiResponse.success(serviceCenterApplicationService.createAppointment(request));
    }

    @PatchMapping("/appointments/{appointmentId}/cancel")
    public ApiResponse<ServiceAppointmentResponse> cancelAppointment(
        @PathVariable Long appointmentId,
        @Valid @RequestBody(required = false) CancelServiceAppointmentRequest request
    ) {
        return ApiResponse.success(serviceCenterApplicationService.cancelAppointment(appointmentId, request));
    }

    @PostMapping("/appointments/{appointmentId}/review")
    public ApiResponse<ProviderReviewResponse> createProviderReview(
        @PathVariable Long appointmentId,
        @Valid @RequestBody CreateProviderReviewRequest request
    ) {
        return ApiResponse.success(serviceCenterApplicationService.createProviderReview(appointmentId, request));
    }
}
