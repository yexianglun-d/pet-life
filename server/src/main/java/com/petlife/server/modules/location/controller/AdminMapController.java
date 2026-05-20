package com.petlife.server.modules.location.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.location.dto.request.AdminGeocodeRequest;
import com.petlife.server.modules.location.dto.response.AmapConfigStatusResponse;
import com.petlife.server.modules.location.dto.response.AmapGeocodeResponse;
import com.petlife.server.modules.location.dto.response.AmapReverseGeocodeResponse;
import com.petlife.server.modules.location.service.AmapLocationApplicationService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台地图运营辅助接口。
 */
@RestController
@RequestMapping("/api/v1/admin/map")
public class AdminMapController {

    private final AmapLocationApplicationService amapLocationApplicationService;

    public AdminMapController(AmapLocationApplicationService amapLocationApplicationService) {
        this.amapLocationApplicationService = amapLocationApplicationService;
    }

    @GetMapping("/config")
    public ApiResponse<AmapConfigStatusResponse> getConfigStatus() {
        return ApiResponse.success(amapLocationApplicationService.getConfigStatus());
    }

    @GetMapping("/geocode")
    public ApiResponse<AmapGeocodeResponse> geocode(@Valid @ModelAttribute AdminGeocodeRequest request) {
        return ApiResponse.success(amapLocationApplicationService.geocode(request));
    }

    @GetMapping("/reverse-geocode")
    public ApiResponse<AmapReverseGeocodeResponse> reverseGeocode(
        @RequestParam("latitude") BigDecimal latitude,
        @RequestParam("longitude") BigDecimal longitude
    ) {
        return ApiResponse.success(amapLocationApplicationService.reverseGeocode(latitude, longitude));
    }
}
