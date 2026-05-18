package com.petlife.server.modules.user.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.user.dto.request.UpdateCurrentPetRequest;
import com.petlife.server.modules.user.dto.request.UpdateUserCityRequest;
import com.petlife.server.modules.user.dto.request.UpdateUserNotificationSettingsRequest;
import com.petlife.server.modules.user.dto.request.UpdateUserProfileRequest;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import com.petlife.server.modules.user.dto.response.UserSettingsResponse;
import com.petlife.server.modules.user.service.UserApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户控制器。
 */
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        return ApiResponse.success(userApplicationService.getCurrentUser());
    }

    @GetMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> getUserSettings() {
        return ApiResponse.success(userApplicationService.getUserSettings());
    }

    @PatchMapping("/me/profile")
    public ApiResponse<UserSettingsResponse> updateUserProfile(
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.success(userApplicationService.updateUserProfile(request.nickname()));
    }

    @PatchMapping("/me/settings/current-pet")
    public ApiResponse<CurrentUserResponse> updateCurrentPet(
        @Valid @RequestBody UpdateCurrentPetRequest request
    ) {
        return ApiResponse.success(userApplicationService.updateCurrentPet(Long.valueOf(request.petId())));
    }

    @PatchMapping("/me/settings/city")
    public ApiResponse<UserSettingsResponse> updateUserCity(
        @Valid @RequestBody UpdateUserCityRequest request
    ) {
        return ApiResponse.success(userApplicationService.updateUserCity(request.cityCode(), request.cityName()));
    }

    @PatchMapping("/me/settings/notifications")
    public ApiResponse<UserSettingsResponse> updateUserNotificationSettings(
        @Valid @RequestBody UpdateUserNotificationSettingsRequest request
    ) {
        return ApiResponse.success(
            userApplicationService.updateUserNotificationSettings(
                request.notificationEnabled(),
                request.privacyLevel()
            )
        );
    }
}
