package com.petlife.server.modules.community.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.community.dto.request.AdminUpdateCommunityContentStatusRequest;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityQuestionDetailResponse;
import com.petlife.server.modules.community.service.CommunityApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台社区内容治理控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/community")
public class AdminCommunityController {

    private final CommunityApplicationService communityApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AdminCommunityController(
        CommunityApplicationService communityApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.communityApplicationService = communityApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<CommunityPostResponse>> listPosts(
        @RequestParam(value = "post_type", required = false) String postType,
        @RequestParam(value = "review_status", required = false) String reviewStatus,
        @RequestParam(value = "visibility", required = false) String visibility,
        @RequestParam(value = "author_user_id", required = false) Long authorUserId,
        @RequestParam(value = "topic_id", required = false) Long topicId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(communityApplicationService.listAdminPosts(
            postType,
            reviewStatus,
            visibility,
            authorUserId,
            topicId,
            keyword
        ));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(@PathVariable Long postId) {
        return ApiResponse.success(communityApplicationService.getAdminPost(postId));
    }

    @PatchMapping("/posts/{postId}/status")
    public ApiResponse<CommunityPostResponse> updatePostStatus(
        @PathVariable Long postId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateCommunityContentStatusRequest request
    ) {
        return ApiResponse.success(
            communityApplicationService.updateAdminPostStatus(
                postId,
                auditContext(operatorName, httpServletRequest),
                request
            )
        );
    }

    @GetMapping("/questions")
    public ApiResponse<List<CommunityPostResponse>> listQuestions(
        @RequestParam(value = "review_status", required = false) String reviewStatus,
        @RequestParam(value = "visibility", required = false) String visibility,
        @RequestParam(value = "author_user_id", required = false) Long authorUserId,
        @RequestParam(value = "topic_id", required = false) Long topicId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(communityApplicationService.listAdminQuestions(
            reviewStatus,
            visibility,
            authorUserId,
            topicId,
            keyword
        ));
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<CommunityQuestionDetailResponse> getQuestion(@PathVariable Long questionId) {
        return ApiResponse.success(communityApplicationService.getAdminQuestion(questionId));
    }

    @PatchMapping("/questions/{questionId}/status")
    public ApiResponse<CommunityPostResponse> updateQuestionStatus(
        @PathVariable Long questionId,
        @RequestHeader(value = "X-Admin-Operator", required = false) String operatorName,
        HttpServletRequest httpServletRequest,
        @Valid @RequestBody AdminUpdateCommunityContentStatusRequest request
    ) {
        return ApiResponse.success(
            communityApplicationService.updateAdminQuestionStatus(
                questionId,
                auditContext(operatorName, httpServletRequest),
                request
            )
        );
    }

    private AdminOperationContext auditContext(String operatorName, HttpServletRequest httpServletRequest) {
        return auditLogApplicationService.resolveAdminOperationContext(operatorName, httpServletRequest);
    }
}
