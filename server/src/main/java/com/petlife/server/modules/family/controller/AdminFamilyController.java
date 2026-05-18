package com.petlife.server.modules.family.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.family.dto.request.AdminRepairFamilyOwnerRequest;
import com.petlife.server.modules.family.dto.request.AdminUpdateFamilyStatusRequest;
import com.petlife.server.modules.family.dto.response.AdminFamilyResponse;
import com.petlife.server.modules.family.service.FamilyApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台家庭查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/families")
public class AdminFamilyController {

    private final FamilyApplicationService familyApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminFamilyController(
        FamilyApplicationService familyApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.familyApplicationService = familyApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
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

    @PatchMapping("/{familyId}/status")
    public ApiResponse<AdminFamilyResponse> updateFamilyStatus(
        @PathVariable Long familyId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateFamilyStatusRequest request
    ) {
        AdminOperationContext operationContext =
            auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
        return ApiResponse.success(familyApplicationService.updateAdminFamilyStatus(familyId, operationContext, request));
    }

    @PostMapping("/{familyId}/owner-member-repair")
    public ApiResponse<AdminFamilyResponse> repairOwnerMember(
        @PathVariable Long familyId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody(required = false) AdminRepairFamilyOwnerRequest request
    ) {
        AdminOperationContext operationContext =
            auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
        return ApiResponse.success(
            familyApplicationService.repairAdminFamilyOwnerMember(
                familyId,
                operationContext,
                request == null ? new AdminRepairFamilyOwnerRequest(null) : request
            )
        );
    }
}
