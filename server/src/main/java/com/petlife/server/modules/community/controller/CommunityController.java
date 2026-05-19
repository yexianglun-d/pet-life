package com.petlife.server.modules.community.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.community.dto.request.CreateCommunityCommentRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityPostRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityReportRequest;
import com.petlife.server.modules.community.dto.response.CommunityCommentResponse;
import com.petlife.server.modules.community.dto.response.CommunityFollowStatusResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityQuestionDetailResponse;
import com.petlife.server.modules.community.dto.response.CommunityReportResponse;
import com.petlife.server.modules.community.dto.response.CommunityTopicDetailResponse;
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
        @RequestParam(value = "tab", required = false) String tab,
        @RequestParam(value = "city_code", required = false) String cityCode
    ) {
        return ApiResponse.success(communityApplicationService.listFeed(tab, cityCode));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostResponse> createPost(
        @Valid @RequestBody CreateCommunityPostRequest request
    ) {
        return ApiResponse.success(communityApplicationService.createPost(request));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.getPost(postId));
    }

    @GetMapping("/topics/{topicId}")
    public ApiResponse<CommunityTopicDetailResponse> getTopic(@PathVariable Long topicId) {
        return ApiResponse.success(communityApplicationService.getTopic(topicId));
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<CommunityQuestionDetailResponse> getQuestion(@PathVariable Long questionId) {
        return ApiResponse.success(communityApplicationService.getQuestion(questionId));
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

    @PostMapping("/users/{userId}/follow")
    public ApiResponse<CommunityFollowStatusResponse> followUser(@PathVariable Long userId) {
        return ApiResponse.success(communityApplicationService.followUser(userId));
    }

    @DeleteMapping("/users/{userId}/follow")
    public ApiResponse<CommunityFollowStatusResponse> unfollowUser(@PathVariable Long userId) {
        return ApiResponse.success(communityApplicationService.unfollowUser(userId));
    }

    @GetMapping("/users/{userId}/follow-status")
    public ApiResponse<CommunityFollowStatusResponse> getFollowStatus(@PathVariable Long userId) {
        return ApiResponse.success(communityApplicationService.getFollowStatus(userId));
    }

    @PostMapping("/posts/{postId}/report")
    public ApiResponse<CommunityReportResponse> reportPost(
        @PathVariable Long postId,
        @Valid @RequestBody CreateCommunityReportRequest request
    ) {
        return ApiResponse.success(communityApplicationService.reportPost(postId, request));
    }
}
