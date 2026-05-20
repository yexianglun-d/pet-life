package com.petlife.server.modules.moderation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.community.persistence.CommunityPersistenceMapper;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostReviewStatusCommand;
import com.petlife.server.modules.moderation.converter.ModerationTaskConverter;
import com.petlife.server.modules.moderation.domain.entity.ModerationTaskEntity;
import com.petlife.server.modules.moderation.dto.request.AdminReviewModerationTaskRequest;
import com.petlife.server.modules.moderation.dto.request.ModerationProviderCallbackRequest;
import com.petlife.server.modules.moderation.dto.response.ModerationTaskResponse;
import com.petlife.server.modules.moderation.persistence.ModerationTaskPersistenceMapper;
import com.petlife.server.modules.moderation.persistence.command.CreateModerationTaskCommand;
import com.petlife.server.modules.moderation.persistence.command.UpdateModerationTaskReviewCommand;
import com.petlife.server.modules.moderation.service.provider.ContentModerationProvider;
import com.petlife.server.modules.moderation.service.provider.DevelopmentNoopContentModerationProvider;
import com.petlife.server.modules.moderation.service.provider.ModerationSubmissionRequest;
import com.petlife.server.modules.moderation.service.provider.ModerationSubmissionResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内容审核任务应用服务。
 */
@Service
public class ModerationTaskApplicationService {

    public static final String TARGET_TYPE_COMMUNITY_POST = "community_post";
    public static final String TARGET_TYPE_COMMUNITY_QUESTION = "community_question";
    private static final String REVIEW_STATUS_PENDING = "pending";
    private static final String REVIEW_STATUS_APPROVED = "approved";
    private static final String REVIEW_STATUS_REJECTED = "rejected";
    private static final String REVIEW_STATUS_FAILED = "failed";
    private static final String POST_REVIEW_PENDING = "pending_review";
    private static final String POST_REVIEW_APPROVED = "approved";
    private static final String POST_REVIEW_REJECTED = "rejected";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String AUDIT_TARGET_TYPE_MODERATION_TASK = "moderation_task";
    private static final String FAILURE_REASON_CONTENT_UPDATED = "content_updated_before_review";
    private static final Set<String> SUPPORTED_TARGET_TYPES = Set.of(
        TARGET_TYPE_COMMUNITY_POST,
        TARGET_TYPE_COMMUNITY_QUESTION
    );
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "text",
        "image_text",
        "video",
        "qa"
    );
    private static final Set<String> SUPPORTED_REVIEW_STATUSES = Set.of(
        REVIEW_STATUS_PENDING,
        REVIEW_STATUS_APPROVED,
        REVIEW_STATUS_REJECTED,
        REVIEW_STATUS_FAILED
    );
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(ACTION_APPROVE, ACTION_REJECT);

    private final ModerationTaskPersistenceMapper moderationTaskPersistenceMapper;
    private final ModerationTaskConverter moderationTaskConverter;
    private final CommunityPersistenceMapper communityPersistenceMapper;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ObjectMapper objectMapper;
    private final Map<String, ContentModerationProvider> providers;

    public ModerationTaskApplicationService(
        ModerationTaskPersistenceMapper moderationTaskPersistenceMapper,
        ModerationTaskConverter moderationTaskConverter,
        CommunityPersistenceMapper communityPersistenceMapper,
        AuditLogApplicationService auditLogApplicationService,
        ObjectMapper objectMapper,
        List<ContentModerationProvider> providers
    ) {
        this.moderationTaskPersistenceMapper = moderationTaskPersistenceMapper;
        this.moderationTaskConverter = moderationTaskConverter;
        this.communityPersistenceMapper = communityPersistenceMapper;
        this.auditLogApplicationService = auditLogApplicationService;
        this.objectMapper = objectMapper;
        this.providers = providers.stream()
            .collect(Collectors.toUnmodifiableMap(ContentModerationProvider::providerCode, Function.identity()));
    }

    public List<ModerationTaskResponse> listTasks(
        String targetType,
        Long targetId,
        String contentType,
        String reviewStatus,
        String providerCode
    ) {
        return moderationTaskPersistenceMapper
            .listTasks(
                normalizeOptionalTargetType(targetType),
                targetId,
                normalizeOptionalContentType(contentType),
                normalizeOptionalReviewStatus(reviewStatus),
                normalizeOptionalProviderCode(providerCode)
            )
            .stream()
            .map(moderationTaskConverter::toEntity)
            .map(moderationTaskConverter::toResponse)
            .toList();
    }

    public ModerationTaskResponse getTask(Long taskId) {
        return moderationTaskConverter.toResponse(requireTask(taskId));
    }

    @Transactional
    public ModerationTaskResponse createCommunityPostReviewTask(
        Long postId,
        boolean question,
        String contentType,
        Map<String, Object> contentSnapshot
    ) {
        String targetType = question ? TARGET_TYPE_COMMUNITY_QUESTION : TARGET_TYPE_COMMUNITY_POST;
        return moderationTaskConverter.toResponse(
            createReviewTask(targetType, postId, contentType, contentSnapshot)
        );
    }

    @Transactional
    public ModerationTaskResponse reviewTask(
        Long taskId,
        AdminOperationContext operationContext,
        AdminReviewModerationTaskRequest request
    ) {
        ModerationTaskEntity task = requireTask(taskId);
        assertPendingTask(task);
        String action = normalizeAction(request.action());
        String nextReviewStatus = ACTION_APPROVE.equals(action) ? REVIEW_STATUS_APPROVED : REVIEW_STATUS_REJECTED;
        UpdateModerationTaskReviewCommand command = buildReviewCommand(
            taskId,
            nextReviewStatus,
            Map.of(
                "source", "manual",
                "action", action,
                "admin_notes", normalizeAdminNotes(request.adminNotes())
            ),
            request.riskLabels(),
            null,
            Map.of()
        );
        int updatedRows = moderationTaskPersistenceMapper.updatePendingTaskReview(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核任务状态已变化，请刷新后重试");
        }
        applyReviewStatusToTarget(task, nextReviewStatus);
        recordReviewAudit(operationContext, task, action, nextReviewStatus, normalizeAdminNotes(request.adminNotes()));
        return moderationTaskConverter.toResponse(requireTask(taskId));
    }

    @Transactional
    public ModerationTaskResponse handleProviderCallback(
        String providerCode,
        ModerationProviderCallbackRequest request
    ) {
        String normalizedProviderCode = normalizeRequiredProviderCode(providerCode);
        ModerationTaskEntity task = requireTask(request.taskId());
        assertPendingTask(task);
        if (!normalizedProviderCode.equals(task.getProviderCode())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核回调供应商与任务不匹配");
        }

        String reviewStatus = normalizeFinalReviewStatus(request.reviewStatus());
        UpdateModerationTaskReviewCommand command = buildReviewCommand(
            task.getTaskId(),
            reviewStatus,
            request.reviewResult() == null ? Map.of("source", "provider_callback") : request.reviewResult(),
            request.riskLabels(),
            normalizeOptionalText(request.failureReason(), 500, "失败原因不能超过 500 字"),
            request.callbackPayload() == null ? Map.of() : request.callbackPayload()
        );
        int updatedRows = moderationTaskPersistenceMapper.updatePendingTaskReview(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核任务状态已变化，请刷新后重试");
        }
        applyReviewStatusToTarget(task, reviewStatus);
        return moderationTaskConverter.toResponse(requireTask(task.getTaskId()));
    }

    private ModerationTaskEntity createReviewTask(
        String targetType,
        Long targetId,
        String contentType,
        Map<String, Object> contentSnapshot
    ) {
        String normalizedTargetType = normalizeRequiredTargetType(targetType);
        String normalizedContentType = normalizeRequiredContentType(contentType);
        ContentModerationProvider provider = requireProvider(DevelopmentNoopContentModerationProvider.PROVIDER_CODE);
        String snapshotJson = toJson(contentSnapshot == null ? Map.of() : contentSnapshot);
        /*
         * 同一内容更新后，旧的待审核任务不能再被人工通过，否则会把已经变化的内容错误放出。
         * 因此创建新任务前将旧 pending 任务标记为 failed，并保留失败原因用于后台排查。
         */
        moderationTaskPersistenceMapper.failPendingTasksByTarget(
            normalizedTargetType,
            targetId,
            FAILURE_REASON_CONTENT_UPDATED
        );

        ModerationSubmissionResult submissionResult = provider.submit(
            new ModerationSubmissionRequest(normalizedTargetType, targetId, normalizedContentType, snapshotJson)
        );
        CreateModerationTaskCommand command = new CreateModerationTaskCommand();
        command.setTargetType(normalizedTargetType);
        command.setTargetId(targetId);
        command.setContentType(normalizedContentType);
        command.setContentSnapshot(snapshotJson);
        command.setProviderCode(provider.providerCode());
        command.setReviewStatus(normalizeRequiredReviewStatus(submissionResult.reviewStatus()));
        command.setReviewResult(defaultJsonObject(submissionResult.reviewResult()));
        command.setRiskLabels(defaultJsonArray(submissionResult.riskLabels()));
        command.setFailureReason(submissionResult.failureReason());
        command.setCallbackPayload("{}");
        moderationTaskPersistenceMapper.insertTask(command);
        return requireTask(command.getTaskId());
    }

    private void applyReviewStatusToTarget(ModerationTaskEntity task, String reviewStatus) {
        if (!TARGET_TYPE_COMMUNITY_POST.equals(task.getTargetType())
            && !TARGET_TYPE_COMMUNITY_QUESTION.equals(task.getTargetType())) {
            return;
        }
        String nextPostReviewStatus = switch (reviewStatus) {
            case REVIEW_STATUS_APPROVED -> POST_REVIEW_APPROVED;
            case REVIEW_STATUS_REJECTED -> POST_REVIEW_REJECTED;
            default -> POST_REVIEW_PENDING;
        };
        if (POST_REVIEW_PENDING.equals(nextPostReviewStatus)) {
            return;
        }
        UpdateCommunityPostReviewStatusCommand command = new UpdateCommunityPostReviewStatusCommand();
        command.setPostId(task.getTargetId());
        command.setReviewStatus(nextPostReviewStatus);
        int updatedRows = communityPersistenceMapper.updatePostReviewStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
    }

    private ModerationTaskEntity requireTask(Long taskId) {
        ModerationTaskEntity task = moderationTaskConverter.toEntity(
            moderationTaskPersistenceMapper.findTaskById(taskId)
        );
        if (task == null) {
            throw new BusinessException(ResponseCode.MODERATION_TASK_NOT_FOUND);
        }
        return task;
    }

    private void assertPendingTask(ModerationTaskEntity task) {
        if (!REVIEW_STATUS_PENDING.equals(task.getReviewStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核任务不是待处理状态");
        }
    }

    private UpdateModerationTaskReviewCommand buildReviewCommand(
        Long taskId,
        String reviewStatus,
        Map<String, Object> reviewResult,
        List<String> riskLabels,
        String failureReason,
        Map<String, Object> callbackPayload
    ) {
        UpdateModerationTaskReviewCommand command = new UpdateModerationTaskReviewCommand();
        command.setTaskId(taskId);
        command.setReviewStatus(normalizeFinalReviewStatus(reviewStatus));
        command.setReviewResult(toJson(reviewResult == null ? Map.of() : reviewResult));
        command.setRiskLabels(toJson(riskLabels == null ? List.of() : riskLabels));
        command.setFailureReason(failureReason);
        command.setCallbackPayload(toJson(callbackPayload == null ? Map.of() : callbackPayload));
        return command;
    }

    private ContentModerationProvider requireProvider(String providerCode) {
        ContentModerationProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核供应商未配置");
        }
        return provider;
    }

    private void recordReviewAudit(
        AdminOperationContext operationContext,
        ModerationTaskEntity task,
        String action,
        String reviewStatus,
        String adminNotes
    ) {
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            AUDIT_TARGET_TYPE_MODERATION_TASK,
            String.valueOf(task.getTaskId()),
            "moderation_task_" + action,
            Map.of(
                "action", action,
                "target_type", task.getTargetType(),
                "target_id", String.valueOf(task.getTargetId()),
                "review_status_before", task.getReviewStatus(),
                "review_status_after", reviewStatus,
                "admin_notes", adminNotes == null ? "" : adminNotes
            )
        );
    }

    private String normalizeRequiredTargetType(String targetType) {
        String normalizedTargetType = normalizeRequiredText(targetType, "审核目标类型不能为空", 30, "审核目标类型不能超过 30 字符")
            .toLowerCase();
        if (!SUPPORTED_TARGET_TYPES.contains(normalizedTargetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核目标类型不支持");
        }
        return normalizedTargetType;
    }

    private String normalizeOptionalTargetType(String targetType) {
        String normalizedTargetType = normalizeOptionalText(targetType, 30, "审核目标类型不能超过 30 字符");
        if (normalizedTargetType == null) {
            return null;
        }
        normalizedTargetType = normalizedTargetType.toLowerCase();
        if (!SUPPORTED_TARGET_TYPES.contains(normalizedTargetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核目标类型不支持");
        }
        return normalizedTargetType;
    }

    private String normalizeRequiredContentType(String contentType) {
        String normalizedContentType = normalizeRequiredText(contentType, "审核内容类型不能为空", 30, "审核内容类型不能超过 30 字符")
            .toLowerCase();
        if (!SUPPORTED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核内容类型不支持");
        }
        return normalizedContentType;
    }

    private String normalizeOptionalContentType(String contentType) {
        String normalizedContentType = normalizeOptionalText(contentType, 30, "审核内容类型不能超过 30 字符");
        if (normalizedContentType == null) {
            return null;
        }
        normalizedContentType = normalizedContentType.toLowerCase();
        if (!SUPPORTED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核内容类型不支持");
        }
        return normalizedContentType;
    }

    private String normalizeRequiredReviewStatus(String reviewStatus) {
        String normalizedReviewStatus = normalizeRequiredText(reviewStatus, "审核状态不能为空", 20, "审核状态不能超过 20 字符")
            .toLowerCase();
        if (!SUPPORTED_REVIEW_STATUSES.contains(normalizedReviewStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核状态不支持");
        }
        return normalizedReviewStatus;
    }

    private String normalizeOptionalReviewStatus(String reviewStatus) {
        String normalizedReviewStatus = normalizeOptionalText(reviewStatus, 20, "审核状态不能超过 20 字符");
        if (normalizedReviewStatus == null) {
            return null;
        }
        normalizedReviewStatus = normalizedReviewStatus.toLowerCase();
        if (!SUPPORTED_REVIEW_STATUSES.contains(normalizedReviewStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核状态不支持");
        }
        return normalizedReviewStatus;
    }

    private String normalizeFinalReviewStatus(String reviewStatus) {
        String normalizedReviewStatus = normalizeRequiredReviewStatus(reviewStatus);
        if (REVIEW_STATUS_PENDING.equals(normalizedReviewStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核结论不能为 pending");
        }
        return normalizedReviewStatus;
    }

    private String normalizeAction(String action) {
        String normalizedAction = normalizeRequiredText(action, "审核动作不能为空", 20, "审核动作不能超过 20 字符")
            .toLowerCase();
        if (!SUPPORTED_ACTIONS.contains(normalizedAction)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核动作仅支持 approve、reject");
        }
        return normalizedAction;
    }

    private String normalizeRequiredProviderCode(String providerCode) {
        return normalizeRequiredText(providerCode, "审核供应商不能为空", 64, "审核供应商不能超过 64 字符").toLowerCase();
    }

    private String normalizeOptionalProviderCode(String providerCode) {
        String normalizedProviderCode = normalizeOptionalText(providerCode, 64, "审核供应商不能超过 64 字符");
        return normalizedProviderCode == null ? null : normalizedProviderCode.toLowerCase();
    }

    private String normalizeAdminNotes(String adminNotes) {
        return normalizeOptionalText(adminNotes, 500, "管理员备注不能超过 500 字");
    }

    private String normalizeRequiredText(String text, String emptyMessage, int maxLength, String maxLengthMessage) {
        String normalizedText = normalizeOptionalText(text, maxLength, maxLengthMessage);
        if (normalizedText == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, emptyMessage);
        }
        return normalizedText;
    }

    private String normalizeOptionalText(String text, int maxLength, String maxLengthMessage) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = text.trim();
        if (normalizedText.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, maxLengthMessage);
        }
        return normalizedText;
    }

    private String defaultJsonObject(String json) {
        return json == null || json.isBlank() ? "{}" : json;
    }

    private String defaultJsonArray(String json) {
        return json == null || json.isBlank() ? "[]" : json;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "审核任务 JSON 序列化失败");
        }
    }
}
