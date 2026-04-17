package com.petlife.server.modules.pet.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.pet.dto.request.CreatePetRequest;
import com.petlife.server.modules.pet.dto.request.UpdatePetRequest;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.dto.response.PetSummaryResponse;
import com.petlife.server.modules.pet.service.PetApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 宠物控制器。
 */
@RestController
@RequestMapping("/api/v1/pets")
public class PetController {

    private final PetApplicationService petApplicationService;

    public PetController(PetApplicationService petApplicationService) {
        this.petApplicationService = petApplicationService;
    }

    @GetMapping
    public ApiResponse<List<PetDetailResponse>> listPets() {
        return ApiResponse.success(petApplicationService.listPets());
    }

    @PostMapping
    public ApiResponse<PetDetailResponse> createPet(@Valid @RequestBody CreatePetRequest request) {
        return ApiResponse.success(petApplicationService.createPet(request));
    }

    @GetMapping("/{petId}")
    public ApiResponse<PetDetailResponse> getPet(@PathVariable Long petId) {
        return ApiResponse.success(petApplicationService.getPet(petId));
    }

    @PatchMapping("/{petId}")
    public ApiResponse<PetDetailResponse> updatePet(
        @PathVariable Long petId,
        @RequestBody UpdatePetRequest request
    ) {
        return ApiResponse.success(petApplicationService.updatePet(petId, request));
    }

    @GetMapping("/{petId}/summary")
    public ApiResponse<PetSummaryResponse> getPetSummary(@PathVariable Long petId) {
        return ApiResponse.success(petApplicationService.getPetSummary(petId));
    }
}
