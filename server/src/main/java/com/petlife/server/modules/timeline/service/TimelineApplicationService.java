package com.petlife.server.modules.timeline.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.timeline.converter.TimelineEventConverter;
import com.petlife.server.modules.timeline.dto.response.TimelineEventResponse;
import com.petlife.server.modules.timeline.persistence.TimelinePersistenceMapper;
import com.petlife.server.modules.timeline.persistence.command.DeleteTimelineEventCommand;
import com.petlife.server.modules.timeline.persistence.command.UpsertTimelineEventCommand;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成长时间轴应用服务。
 *
 * <p>时间轴是宠物维度的派生读模型，只接受健康记录、萌宠日常等事实源驱动。
 * 这里统一封装事件映射与同步逻辑，避免各个业务模块各自拼接时间轴字段。</p>
 */
@Service
public class TimelineApplicationService {

    private static final String EVENT_TYPE_HEALTH = "health";
    private static final String EVENT_TYPE_DAILY_LOG = "daily_log";
    private static final String SOURCE_TYPE_HEALTH_RECORD = "health_record";
    private static final String SOURCE_TYPE_DAILY_LOG = "daily_log";
    private static final String VISIBILITY_FAMILY = "family";

    private final TimelinePersistenceMapper timelinePersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final TimelineEventConverter timelineEventConverter;

    public TimelineApplicationService(
        TimelinePersistenceMapper timelinePersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        TimelineEventConverter timelineEventConverter
    ) {
        this.timelinePersistenceMapper = timelinePersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.timelineEventConverter = timelineEventConverter;
    }

    public List<TimelineEventResponse> listTimelineEvents(Long petId, String eventType) {
        requireAccessiblePet(petId);
        String normalizedEventType = normalizeEventType(eventType);
        return ("all".equals(normalizedEventType)
                ? timelinePersistenceMapper.listTimelineEventsByPetId(petId)
                : timelinePersistenceMapper.listTimelineEventsByPetIdAndEventType(petId, normalizedEventType))
            .stream()
            .map(timelineEventConverter::toEntity)
            .map(timelineEventConverter::toResponse)
            .toList();
    }

    @Transactional
    public void syncHealthRecordEvent(HealthRecordEntity healthRecord) {
        UpsertTimelineEventCommand command = new UpsertTimelineEventCommand();
        command.setPetId(healthRecord.getPetId());
        command.setEventType(EVENT_TYPE_HEALTH);
        command.setSourceType(SOURCE_TYPE_HEALTH_RECORD);
        command.setSourceId(healthRecord.getHealthRecordId());
        command.setEventTime(healthRecord.getOccurredAt());
        command.setTitle(healthRecord.getTitle());
        command.setSummary(buildHealthSummary(healthRecord));
        command.setCoverUrl(null);
        command.setVisibility(VISIBILITY_FAMILY);
        timelinePersistenceMapper.upsertTimelineEvent(command);
    }

    @Transactional
    public void deleteHealthRecordEvent(Long petId, Long healthRecordId) {
        DeleteTimelineEventCommand command = new DeleteTimelineEventCommand();
        command.setPetId(petId);
        command.setSourceType(SOURCE_TYPE_HEALTH_RECORD);
        command.setSourceId(healthRecordId);
        timelinePersistenceMapper.deleteTimelineEvent(command);
    }

    @Transactional
    public void syncDailyLogEvent(DailyLogEntity dailyLog) {
        UpsertTimelineEventCommand command = new UpsertTimelineEventCommand();
        command.setPetId(dailyLog.getPetId());
        command.setEventType(EVENT_TYPE_DAILY_LOG);
        command.setSourceType(SOURCE_TYPE_DAILY_LOG);
        command.setSourceId(dailyLog.getDailyLogId());
        command.setEventTime(dailyLog.getHappenedAt());
        command.setTitle(buildDailyLogTitle(dailyLog.getContent()));
        command.setSummary(dailyLog.getContent());
        command.setCoverUrl(null);
        command.setVisibility(dailyLog.getVisibility());
        timelinePersistenceMapper.upsertTimelineEvent(command);
    }

    @Transactional
    public void deleteDailyLogEvent(Long petId, Long dailyLogId) {
        DeleteTimelineEventCommand command = new DeleteTimelineEventCommand();
        command.setPetId(petId);
        command.setSourceType(SOURCE_TYPE_DAILY_LOG);
        command.setSourceId(dailyLogId);
        timelinePersistenceMapper.deleteTimelineEvent(command);
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank() || "all".equals(eventType.trim())) {
            return "all";
        }
        String normalizedEventType = eventType.trim();
        if (EVENT_TYPE_HEALTH.equals(normalizedEventType) || EVENT_TYPE_DAILY_LOG.equals(normalizedEventType)) {
            return normalizedEventType;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "时间轴事件类型仅支持 all、health 或 daily_log");
    }

    private String buildHealthSummary(HealthRecordEntity healthRecord) {
        if (healthRecord.getResultSummary() != null && healthRecord.getNotes() != null) {
            return healthRecord.getResultSummary() + " · " + healthRecord.getNotes();
        }
        if (healthRecord.getResultSummary() != null) {
            return healthRecord.getResultSummary();
        }
        return healthRecord.getNotes();
    }

    /**
     * 萌宠日常原始内容可能很长，时间轴标题只保留一行可扫读信息，
     * 详情内容仍通过 summary 和详情跳转承接，避免时间轴列表被长文本撑坏层级。
     */
    private String buildDailyLogTitle(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.length() <= 18) {
            return normalizedContent;
        }
        return normalizedContent.substring(0, 18) + "...";
    }
}
