package com.petlife.server.modules.dailylog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.dailylog.persistence.record.DailyLogPersistenceRecord;
import com.petlife.server.modules.dailylog.dto.request.CreateDailyLogRequest;
import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import java.time.LocalDateTime;
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

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final ObjectMapper objectMapper;

    public DailyLogApplicationService(
        DailyLogPersistenceMapper dailyLogPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        ObjectMapper objectMapper
    ) {
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.objectMapper = objectMapper;
    }

    public List<DailyLogResponse> listDailyLogs(Long petId) {
        requireAccessiblePet(petId);
        return dailyLogPersistenceMapper.listDailyLogsByPetId(petId).stream()
            .map(this::toDailyLogResponse)
            .toList();
    }

    @Transactional
    public DailyLogResponse createDailyLog(Long petId, CreateDailyLogRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(petId);
        dailyLogPersistenceMapper.insertDailyLog(
            petId,
            currentUserId,
            request.content(),
            toJson(request.tags() == null ? List.of() : request.tags()),
            request.visibility(),
            DateTimeConverters.toLocalDateTime(request.happenedAt(), LocalDateTime.now())
        );
        DailyLogPersistenceRecord dailyLog =
            dailyLogPersistenceMapper.findDailyLogById(dailyLogPersistenceMapper.selectLastInsertId());
        return toDailyLogResponse(dailyLog);
    }

    private void requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petPersistenceMapper.findAccessiblePetById(currentUserId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private DailyLogResponse toDailyLogResponse(DailyLogPersistenceRecord dailyLog) {
        return new DailyLogResponse(
            String.valueOf(dailyLog.dailyLogId()),
            String.valueOf(dailyLog.petId()),
            dailyLog.content(),
            fromJson(dailyLog.tagsJson()),
            dailyLog.visibility(),
            DateTimeConverters.toOffsetDateTime(dailyLog.happenedAt()),
            DateTimeConverters.toOffsetDateTime(dailyLog.createdAt())
        );
    }

    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "萌宠日常标签格式不合法");
        }
    }

    private List<String> fromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(tagsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
