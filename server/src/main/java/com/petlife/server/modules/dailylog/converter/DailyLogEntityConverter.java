package com.petlife.server.modules.dailylog.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import com.petlife.server.modules.dailylog.persistence.dataobject.DailyLogDataObject;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 萌宠日常实体转换器。
 */
@Component
public class DailyLogEntityConverter {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public DailyLogEntityConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DailyLogEntity toEntity(DailyLogDataObject dailyLogDataObject) {
        if (dailyLogDataObject == null) {
            return null;
        }

        return new DailyLogEntity(
            dailyLogDataObject.dailyLogId(),
            dailyLogDataObject.petId(),
            dailyLogDataObject.authorUserId(),
            dailyLogDataObject.content(),
            fromJson(dailyLogDataObject.tagsJson()),
            dailyLogDataObject.visibility(),
            dailyLogDataObject.happenedAt(),
            dailyLogDataObject.createdAt()
        );
    }

    public DailyLogResponse toResponse(DailyLogEntity dailyLog) {
        return new DailyLogResponse(
            String.valueOf(dailyLog.getDailyLogId()),
            String.valueOf(dailyLog.getPetId()),
            dailyLog.getContent(),
            dailyLog.getTags(),
            dailyLog.getVisibility(),
            DateTimeConverters.toOffsetDateTime(dailyLog.getHappenedAt()),
            DateTimeConverters.toOffsetDateTime(dailyLog.getCreatedAt())
        );
    }

    /**
     * 标签以 JSON 数组落库，保证数据库与前端之间只有一处序列化规则。
     */
    public String toTagsJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "萌宠日常标签格式不合法");
        }
    }

    private List<String> fromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }

        try {
            return List.copyOf(objectMapper.readValue(tagsJson, STRING_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("pet_daily_logs.scene_tags 数据格式不合法", ex);
        }
    }
}
