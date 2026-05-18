package com.petlife.server.modules.admin.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.dto.request.AdminLoginRequest;
import com.petlife.server.modules.admin.dto.request.AdminLogoutRequest;
import com.petlife.server.modules.admin.dto.request.AdminRefreshTokenRequest;
import com.petlife.server.modules.admin.dto.response.AdminLoginResponse;
import com.petlife.server.modules.admin.dto.response.AdminRefreshTokenResponse;
import com.petlife.server.modules.admin.service.AdminAuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台认证控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    public AdminAuthController(AdminAuthApplicationService adminAuthApplicationService) {
        this.adminAuthApplicationService = adminAuthApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthApplicationService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AdminRefreshTokenResponse> refreshToken(
        @Valid @RequestBody AdminRefreshTokenRequest request
    ) {
        return ApiResponse.success(adminAuthApplicationService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody AdminLogoutRequest request) {
        adminAuthApplicationService.logout(request);
        return ApiResponse.success(null);
    }
}
