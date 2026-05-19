package com.petlife.server.modules.auth.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.auth.dto.request.AuthLogoutRequest;
import com.petlife.server.modules.auth.dto.request.AuthRefreshTokenRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsLoginRequest;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.dto.response.AuthRefreshTokenResponse;
import com.petlife.server.modules.auth.dto.response.AuthLoginSmsResponse;
import com.petlife.server.modules.auth.dto.response.AuthSmsSendResponse;
import com.petlife.server.modules.auth.service.AuthApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/sms/send")
    public ApiResponse<AuthSmsSendResponse> sendSmsCode(
        @Valid @RequestBody AuthSmsSendRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authApplicationService.sendSmsCode(request, httpServletRequest));
    }

    @PostMapping("/login/sms")
    public ApiResponse<AuthLoginSmsResponse> loginBySms(@Valid @RequestBody AuthSmsLoginRequest request) {
        return ApiResponse.success(authApplicationService.loginBySms(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthRefreshTokenResponse> refreshToken(
        @Valid @RequestBody AuthRefreshTokenRequest request
    ) {
        return ApiResponse.success(authApplicationService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody AuthLogoutRequest request) {
        authApplicationService.logout(request);
        return ApiResponse.success(null);
    }
}
