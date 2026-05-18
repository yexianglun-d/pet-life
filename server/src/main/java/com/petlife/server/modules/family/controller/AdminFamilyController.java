package com.petlife.server.modules.family.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.family.dto.response.AdminFamilyResponse;
import com.petlife.server.modules.family.service.FamilyApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台家庭查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/families")
public class AdminFamilyController {

    private final FamilyApplicationService familyApplicationService;

    public AdminFamilyController(FamilyApplicationService familyApplicationService) {
        this.familyApplicationService = familyApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminFamilyResponse>> listFamilies(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "family_name", required = false) String familyName,
        @RequestParam(value = "member_mobile", required = false) String memberMobile,
        @RequestParam(value = "member_role", required = false) String memberRole,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        return ApiResponse.success(
            familyApplicationService.listAdminFamilies(keyword, familyName, memberMobile, memberRole, status)
        );
    }

    @GetMapping("/{familyId}")
    public ApiResponse<AdminFamilyResponse> getFamily(@PathVariable Long familyId) {
        return ApiResponse.success(familyApplicationService.getAdminFamily(familyId));
    }
}
