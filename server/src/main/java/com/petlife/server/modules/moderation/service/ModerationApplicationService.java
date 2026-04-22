package com.petlife.server.modules.moderation.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.moderation.converter.ModerationReportConverter;
import com.petlife.server.modules.moderation.domain.entity.ModerationReportEntity;
import com.petlife.server.modules.moderation.dto.request.ProcessModerationReportRequest;
import com.petlife.server.modules.moderation.dto.response.ModerationReportResponse;
import com.petlife.server.modules.moderation.persistence.ModerationPersistenceMapper;
import com.petlife.server.modules.moderation.persistence.command.ProcessModerationReportCommand;
import com.petlife.server.modules.moderation.persistence.command.UpdateModerationTargetPostReviewStatusCommand;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审核中心应用服务。
 */
@Service
public class ModerationApplicationService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSED = "processed";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_ALL = "all";
    private static final String ACTION_CONFIRM_VIOLATION = "confirm_violation";
    private static final String ACTION_DISMISS_REPORT = "dismiss_report";
    private static final String REVIEW_STATUS_REJECTED = "rejected";
    private static final String TARGET_TYPE_POST = "post";
    private static final String DEFAULT_OPERATOR = "admin-console";
    private static final Set<String> SUPPORTED_STATUS_FILTERS = Set.of(
        STATUS_ALL,
        STATUS_PENDING,
        STATUS_PROCESSED,
        STATUS_REJECTED
    );
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        ACTION_CONFIRM_VIOLATION,
        ACTION_DISMISS_REPORT
    );

    private final ModerationPersistenceMapper moderationPersistenceMapper;
    private final ModerationReportConverter moderationReportConverter;

    public ModerationApplicationService(
        ModerationPersistenceMapper moderationPersistenceMapper,
        ModerationReportConverter moderationReportConverter
    ) {
        this.moderationPersistenceMapper = moderationPersistenceMapper;
        this.moderationReportConverter = moderationReportConverter;
    }

    public List<ModerationReportResponse> listReports(String status) {
        String normalizedStatus = normalizeStatusFilter(status);
        String persistenceStatus = STATUS_ALL.equals(normalizedStatus) ? null : normalizedStatus;
        return moderationPersistenceMapper.listReports(persistenceStatus).stream()
            .map(moderationReportConverter::toEntity)
            .map(moderationReportConverter::toResponse)
            .toList();
    }

    @Transactional
    public ModerationReportResponse processReport(
        Long reportId,
        String operatorName,
        ProcessModerationReportRequest request
    ) {
        ModerationReportEntity moderationReport = requireReport(reportId);
        if (!STATUS_PENDING.equals(moderationReport.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该举报已处理，无需重复操作");
        }

        String normalizedAction = normalizeAction(request.action());
        String processedStatus = ACTION_CONFIRM_VIOLATION.equals(normalizedAction)
            ? STATUS_PROCESSED
            : STATUS_REJECTED;

        ProcessModerationReportCommand processCommand = new ProcessModerationReportCommand();
        processCommand.setReportId(reportId);
        processCommand.setStatus(processedStatus);
        processCommand.setProcessedBy(normalizeOperatorName(operatorName));
        int updatedRows = moderationPersistenceMapper.processReport(processCommand);
        if (updatedRows <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "举报处理失败，请刷新后重试");
        }

        /**
         * 举报处理的目标是“关闭待处理队列”，而不是依赖目标帖子一定还在线。
         * 因此即使内容已被作者撤回，也允许关闭举报；只有在帖子仍存在时才下沉审核结论。
         */
        if (ACTION_CONFIRM_VIOLATION.equals(normalizedAction)
            && TARGET_TYPE_POST.equals(moderationReport.getTargetType())
            && moderationReport.getPostId() != null
            && !moderationReport.isPostDeleted()) {
            UpdateModerationTargetPostReviewStatusCommand updateCommand =
                new UpdateModerationTargetPostReviewStatusCommand();
            updateCommand.setPostId(moderationReport.getPostId());
            updateCommand.setReviewStatus(REVIEW_STATUS_REJECTED);
            moderationPersistenceMapper.updateTargetPostReviewStatus(updateCommand);
        }

        return moderationReportConverter.toResponse(requireReport(reportId));
    }

    private ModerationReportEntity requireReport(Long reportId) {
        ModerationReportEntity moderationReport = moderationReportConverter.toEntity(
            moderationPersistenceMapper.findReportById(reportId)
        );
        if (moderationReport == null) {
            throw new BusinessException(ResponseCode.MODERATION_REPORT_NOT_FOUND);
        }
        return moderationReport;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ALL;
        }
        String normalizedStatus = status.trim().toLowerCase();
        if (!SUPPORTED_STATUS_FILTERS.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核列表状态筛选不支持");
        }
        return normalizedStatus;
    }

    private String normalizeAction(String action) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase();
        if (!SUPPORTED_ACTIONS.contains(normalizedAction)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核处理动作不支持");
        }
        return normalizedAction;
    }

    private String normalizeOperatorName(String operatorName) {
        if (operatorName == null || operatorName.isBlank()) {
            return DEFAULT_OPERATOR;
        }
        String normalizedOperatorName = operatorName.trim();
        if (normalizedOperatorName.length() > 64) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "处理人标识不能超过 64 字");
        }
        return normalizedOperatorName;
    }
}
