package com.petlife.server.modules.moderation.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.moderation.dto.request.ModerationProviderCallbackRequest;
import com.petlife.server.modules.moderation.dto.response.ModerationTaskResponse;
import com.petlife.server.modules.moderation.service.ModerationTaskApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容审核供应商回调入口。
 */
@RestController
@RequestMapping("/api/v1/moderation/callbacks")
public class ModerationCallbackController {

    private final ModerationTaskApplicationService moderationTaskApplicationService;

    public ModerationCallbackController(ModerationTaskApplicationService moderationTaskApplicationService) {
        this.moderationTaskApplicationService = moderationTaskApplicationService;
    }

    @PostMapping("/{providerCode}")
    public ApiResponse<ModerationTaskResponse> handleCallback(
        @PathVariable String providerCode,
        @Valid @RequestBody ModerationProviderCallbackRequest request
    ) {
        return ApiResponse.success(moderationTaskApplicationService.handleProviderCallback(providerCode, request));
    }
}
