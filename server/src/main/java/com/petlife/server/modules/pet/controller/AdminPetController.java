package com.petlife.server.modules.pet.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.pet.dto.request.AdminRepairPetRequest;
import com.petlife.server.modules.pet.dto.response.AdminPetResponse;
import com.petlife.server.modules.pet.service.PetApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台宠物查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/pets")
public class AdminPetController {

    private final PetApplicationService petApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminPetController(
        PetApplicationService petApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.petApplicationService = petApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminPetResponse>> listPets(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pet_name", required = false) String petName,
        @RequestParam(value = "pet_type", required = false) String petType,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "owner_mobile", required = false) String ownerMobile,
        @RequestParam(value = "family_id", required = false) Long familyId
    ) {
        return ApiResponse.success(
            petApplicationService.listAdminPets(keyword, petName, petType, status, ownerMobile, familyId)
        );
    }

    @GetMapping("/{petId}")
    public ApiResponse<AdminPetResponse> getPet(@PathVariable Long petId) {
        return ApiResponse.success(petApplicationService.getAdminPet(petId));
    }

    @PostMapping("/{petId}/repair")
    public ApiResponse<AdminPetResponse> repairPet(
        @PathVariable Long petId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminRepairPetRequest request
    ) {
        AdminOperationContext operationContext =
            auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
        return ApiResponse.success(petApplicationService.repairAdminPet(petId, operationContext, request));
    }
}
