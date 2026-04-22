package com.petlife.server.modules.dailylog.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.community.service.CommunityApplicationService;
import com.petlife.server.modules.dailylog.converter.DailyLogEntityConverter;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.dto.request.CreateDailyLogRequest;
import com.petlife.server.modules.dailylog.dto.request.UpdateDailyLogRequest;
import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.dailylog.persistence.command.CreateDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.DeleteDailyLogCommand;
import com.petlife.server.modules.dailylog.persistence.command.UpdateDailyLogCommunityBindingCommand;
import com.petlife.server.modules.dailylog.persistence.command.UpdateDailyLogCommand;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 萌宠日常应用服务。
 *
 * <p>萌宠日常既要服务用户私有记录，又要为后续社区公开发布提供内容来源，因此在服务层统一处理
 * 内容、标签和可见范围映射，避免前端被迫感知后续领域拆分。</p>
 */
@Service
public class DailyLogApplicationService {

    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final DailyLogEntityConverter dailyLogEntityConverter;
    private final TimelineApplicationService timelineApplicationService;
    private final CommunityApplicationService communityApplicationService;

    public DailyLogApplicationService(
        DailyLogPersistenceMapper dailyLogPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        DailyLogEntityConverter dailyLogEntityConverter,
        TimelineApplicationService timelineApplicationService,
        CommunityApplicationService communityApplicationService
    ) {
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
        this.timelineApplicationService = timelineApplicationService;
        this.communityApplicationService = communityApplicationService;
    }

    public List<DailyLogResponse> listDailyLogs(Long petId) {
        requireAccessiblePet(petId);
        return dailyLogPersistenceMapper.listDailyLogsByPetId(petId).stream()
            .map(dailyLogEntityConverter::toEntity)
            .map(dailyLogEntityConverter::toResponse)
            .toList();
    }

    public DailyLogResponse getDailyLog(Long petId, Long dailyLogId) {
        requireAccessiblePet(petId);
        return dailyLogEntityConverter.toResponse(requireDailyLog(petId, dailyLogId));
    }

    @Transactional
    public DailyLogResponse createDailyLog(Long petId, CreateDailyLogRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        CreateDailyLogCommand command = buildCreateDailyLogCommand(petId, currentUserId, request);
        dailyLogPersistenceMapper.insertDailyLog(command);
        DailyLogEntity dailyLog = dailyLogEntityConverter.toEntity(dailyLogPersistenceMapper.findDailyLogById(command.getId()));
        dailyLog = syncDerivedModels(dailyLog);
        return dailyLogEntityConverter.toResponse(dailyLog);
    }

    @Transactional
    public DailyLogResponse updateDailyLog(Long petId, Long dailyLogId, UpdateDailyLogRequest request) {
        requireAccessiblePet(petId);
        requireDailyLog(petId, dailyLogId);
        UpdateDailyLogCommand command = buildUpdateDailyLogCommand(petId, dailyLogId, request);
        int updatedRows = dailyLogPersistenceMapper.updateDailyLog(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.DAILY_LOG_NOT_FOUND);
        }
        DailyLogEntity dailyLog = requireDailyLog(petId, dailyLogId);
        dailyLog = syncDerivedModels(dailyLog);
        return dailyLogEntityConverter.toResponse(dailyLog);
    }

    @Transactional
    public void deleteDailyLog(Long petId, Long dailyLogId) {
        requireAccessiblePet(petId);
        DailyLogEntity dailyLog = requireDailyLog(petId, dailyLogId);
        DeleteDailyLogCommand command = new DeleteDailyLogCommand();
        command.setPetId(petId);
        command.setDailyLogId(dailyLogId);
        int updatedRows = dailyLogPersistenceMapper.deleteDailyLog(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.DAILY_LOG_NOT_FOUND);
        }
        if (dailyLog.getCommunityPostId() != null) {
            communityApplicationService.removeDailyLogPost(dailyLog.getCommunityPostId(), dailyLog.getDailyLogId());
        }
        timelineApplicationService.deleteDailyLogEvent(petId, dailyLogId);
    }

    private DailyLogEntity syncDerivedModels(DailyLogEntity dailyLog) {
        timelineApplicationService.syncDailyLogEvent(dailyLog);
        return syncCommunityDistribution(dailyLog);
    }

    /**
     * 社区帖子是萌宠日常的派生分发表，不允许两边各自独立编辑。
     * 这里统一处理“发布、撤回、修复脏绑定”三种状态，避免日常和社区出现双写漂移。
     */
    private DailyLogEntity syncCommunityDistribution(DailyLogEntity dailyLog) {
        if (dailyLog.isSyncToCommunity()) {
            Long communityPostId = communityApplicationService.publishDailyLog(dailyLog);
            if (!communityPostId.equals(dailyLog.getCommunityPostId())) {
                updateCommunityBinding(dailyLog.getDailyLogId(), communityPostId, true);
                return requireDailyLog(dailyLog.getPetId(), dailyLog.getDailyLogId());
            }
            return dailyLog;
        }

        if (dailyLog.getCommunityPostId() != null) {
            communityApplicationService.removeDailyLogPost(dailyLog.getCommunityPostId(), dailyLog.getDailyLogId());
            updateCommunityBinding(dailyLog.getDailyLogId(), null, false);
            return requireDailyLog(dailyLog.getPetId(), dailyLog.getDailyLogId());
        }
        return dailyLog;
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private DailyLogEntity requireDailyLog(Long petId, Long dailyLogId) {
        DailyLogEntity dailyLog = dailyLogEntityConverter.toEntity(
            dailyLogPersistenceMapper.findDailyLogByPetIdAndId(petId, dailyLogId)
        );
        if (dailyLog == null) {
            throw new BusinessException(ResponseCode.DAILY_LOG_NOT_FOUND);
        }
        return dailyLog;
    }

    private CreateDailyLogCommand buildCreateDailyLogCommand(
        Long petId,
        Long authorUserId,
        CreateDailyLogRequest request
    ) {
        CreateDailyLogCommand command = new CreateDailyLogCommand();
        command.setPetId(petId);
        command.setAuthorUserId(authorUserId);
        command.setContent(normalizeContent(request.content()));
        command.setTagsJson(dailyLogEntityConverter.toTagsJson(normalizeTags(request.tags())));
        String visibility = normalizeVisibility(request.visibility());
        command.setVisibility(visibility);
        command.setSyncToCommunity(normalizeSyncToCommunity(request.syncToCommunity(), visibility));
        command.setHappenedAt(normalizeHappenedAt(request.happenedAt()));
        return command;
    }

    private UpdateDailyLogCommand buildUpdateDailyLogCommand(
        Long petId,
        Long dailyLogId,
        UpdateDailyLogRequest request
    ) {
        UpdateDailyLogCommand command = new UpdateDailyLogCommand();
        command.setPetId(petId);
        command.setDailyLogId(dailyLogId);
        command.setContent(normalizeContent(request.content()));
        command.setTagsJson(dailyLogEntityConverter.toTagsJson(normalizeTags(request.tags())));
        String visibility = normalizeVisibility(request.visibility());
        command.setVisibility(visibility);
        command.setSyncToCommunity(normalizeSyncToCommunity(request.syncToCommunity(), visibility));
        command.setHappenedAt(normalizeHappenedAt(request.happenedAt()));
        return command;
    }

    private String normalizeContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "日常内容不能为空");
        }
        return normalizedContent;
    }

    /**
     * 标签会同时参与后续时间轴展示和社区内容组织，应用层先去空白并去重，
     * 避免同一条日常因为输入噪声沉淀出无意义的重复标签。
     */
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
            .map(tag -> tag == null ? "" : tag.trim())
            .filter(tag -> !tag.isEmpty())
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private String normalizeVisibility(String visibility) {
        String normalizedVisibility = visibility == null ? "private" : visibility.trim();
        if ("private".equals(normalizedVisibility)
            || "family".equals(normalizedVisibility)
            || "public".equals(normalizedVisibility)) {
            return normalizedVisibility;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "可见范围仅支持 private、family 或 public");
    }

    /**
     * 当前阶段社区只承接公开内容，同步开关必须和可见范围一起校验，
     * 否则会出现“用户以为仅家庭可见，社区却仍然可见”的数据越权问题。
     */
    private boolean normalizeSyncToCommunity(Boolean syncToCommunity, String visibility) {
        boolean normalizedValue = Boolean.TRUE.equals(syncToCommunity);
        if (normalizedValue && !"public".equals(visibility)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "仅公开内容支持同步到社区");
        }
        return normalizedValue;
    }

    private LocalDateTime normalizeHappenedAt(OffsetDateTime happenedAt) {
        return DateTimeConverters.toLocalDateTime(happenedAt, LocalDateTime.now());
    }

    private void updateCommunityBinding(Long dailyLogId, Long communityPostId, boolean syncToCommunity) {
        UpdateDailyLogCommunityBindingCommand command = new UpdateDailyLogCommunityBindingCommand();
        command.setDailyLogId(dailyLogId);
        command.setCommunityPostId(communityPostId);
        command.setSyncToCommunity(syncToCommunity);
        dailyLogPersistenceMapper.updateCommunityBinding(command);
    }

}
