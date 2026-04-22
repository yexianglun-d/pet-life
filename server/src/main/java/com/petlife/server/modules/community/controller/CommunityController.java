package com.petlife.server.modules.community.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.community.dto.request.CreateCommunityCommentRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityReportRequest;
import com.petlife.server.modules.community.dto.response.CommunityCommentResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityReportResponse;
import com.petlife.server.modules.community.service.CommunityApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区接口控制器。
 */
@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

    private final CommunityApplicationService communityApplicationService;

    public CommunityController(CommunityApplicationService communityApplicationService) {
        this.communityApplicationService = communityApplicationService;
    }

    @GetMapping("/feed")
    public ApiResponse<List<CommunityPostResponse>> listFeed(
        @RequestParam(value = "tab", required = false) String tab
    ) {
        return ApiResponse.success(communityApplicationService.listFeed(tab));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.getPost(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommunityCommentResponse>> listComments(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.listComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentResponse> createComment(
        @PathVariable Long postId,
        @Valid @RequestBody CreateCommunityCommentRequest request
    ) {
        return ApiResponse.success(communityApplicationService.createComment(postId, request));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<CommunityPostResponse> likePost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.likePost(postId));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<CommunityPostResponse> unlikePost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.unlikePost(postId));
    }

    @PostMapping("/posts/{postId}/favorite")
    public ApiResponse<CommunityPostResponse> favoritePost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.favoritePost(postId));
    }

    @DeleteMapping("/posts/{postId}/favorite")
    public ApiResponse<CommunityPostResponse> unfavoritePost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.unfavoritePost(postId));
    }

    @PostMapping("/posts/{postId}/report")
    public ApiResponse<CommunityReportResponse> reportPost(
        @PathVariable Long postId,
        @Valid @RequestBody CreateCommunityReportRequest request
    ) {
        return ApiResponse.success(communityApplicationService.reportPost(postId, request));
    }
}
