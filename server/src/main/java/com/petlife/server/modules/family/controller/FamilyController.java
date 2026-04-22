package com.petlife.server.modules.family.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.family.dto.request.CreateFamilyInvitationRequest;
import com.petlife.server.modules.family.dto.request.CreateFamilyRequest;
import com.petlife.server.modules.family.dto.request.UpdateFamilyMemberRoleRequest;
import com.petlife.server.modules.family.dto.response.FamilyDetailResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationPreviewResponse;
import com.petlife.server.modules.family.dto.response.FamilyMemberResponse;
import com.petlife.server.modules.family.service.FamilyApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭共养控制器。
 */
@RestController
@RequestMapping("/api/v1/family")
public class FamilyController {

    private final FamilyApplicationService familyApplicationService;

    public FamilyController(FamilyApplicationService familyApplicationService) {
        this.familyApplicationService = familyApplicationService;
    }

    @GetMapping
    public ApiResponse<FamilyDetailResponse> getCurrentFamily() {
        return ApiResponse.success(familyApplicationService.getCurrentFamily());
    }

    @PostMapping
    public ApiResponse<FamilyDetailResponse> initializeFamily(
        @RequestBody(required = false) CreateFamilyRequest request
    ) {
        return ApiResponse.success(
            familyApplicationService.initializeFamily(
                request == null ? new CreateFamilyRequest(null) : request
            )
        );
    }

    @PostMapping("/invitations")
    public ApiResponse<FamilyInvitationResponse> createInvitation(
        @Valid @RequestBody CreateFamilyInvitationRequest request
    ) {
        return ApiResponse.success(familyApplicationService.createInvitation(request));
    }

    @GetMapping("/invitations/{inviteCode}")
    public ApiResponse<FamilyInvitationPreviewResponse> getInvitationPreview(@PathVariable String inviteCode) {
        return ApiResponse.success(familyApplicationService.getInvitationPreview(inviteCode));
    }

    @PostMapping("/invitations/{inviteCode}/accept")
    public ApiResponse<FamilyDetailResponse> acceptInvitation(@PathVariable String inviteCode) {
        return ApiResponse.success(familyApplicationService.acceptInvitation(inviteCode));
    }

    @PostMapping("/invitations/{inviteCode}/reject")
    public ApiResponse<FamilyInvitationPreviewResponse> rejectInvitation(@PathVariable String inviteCode) {
        return ApiResponse.success(familyApplicationService.rejectInvitation(inviteCode));
    }

    @PatchMapping("/members/{memberId}/role")
    public ApiResponse<FamilyMemberResponse> updateMemberRole(
        @PathVariable Long memberId,
        @Valid @RequestBody UpdateFamilyMemberRoleRequest request
    ) {
        return ApiResponse.success(familyApplicationService.updateMemberRole(memberId, request));
    }

    @DeleteMapping("/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable Long memberId) {
        familyApplicationService.removeMember(memberId);
        return ApiResponse.success(null);
    }
}
