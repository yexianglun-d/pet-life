package com.petlife.server.modules.user.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.user.dto.response.AdminUserResponse;
import com.petlife.server.modules.user.service.UserApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserApplicationService userApplicationService;

    public AdminUserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> listUsers(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "mobile", required = false) String mobile,
        @RequestParam(value = "nickname", required = false) String nickname,
        @RequestParam(value = "city_code", required = false) String cityCode,
        @RequestParam(value = "notification_enabled", required = false) Boolean notificationEnabled,
        @RequestParam(value = "privacy_level", required = false) String privacyLevel
    ) {
        return ApiResponse.success(
            userApplicationService.listAdminUsers(
                keyword,
                mobile,
                nickname,
                cityCode,
                notificationEnabled,
                privacyLevel
            )
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userApplicationService.getAdminUser(userId));
    }
}
