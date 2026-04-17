package com.petlife.server.modules.user.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.user.dto.request.UpdateCurrentPetRequest;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
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

    @PatchMapping("/me/settings/current-pet")
    public ApiResponse<CurrentUserResponse> updateCurrentPet(
        @Valid @RequestBody UpdateCurrentPetRequest request
    ) {
        return ApiResponse.success(userApplicationService.updateCurrentPet(Long.valueOf(request.petId())));
    }
}
