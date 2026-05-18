package com.petlife.server.modules.pet.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.pet.dto.response.AdminPetResponse;
import com.petlife.server.modules.pet.service.PetApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台宠物查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/pets")
public class AdminPetController {

    private final PetApplicationService petApplicationService;

    public AdminPetController(PetApplicationService petApplicationService) {
        this.petApplicationService = petApplicationService;
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
}
