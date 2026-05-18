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
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
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
            fromJson(dailyLogDataObject.mediaListJson(), "pet_daily_logs.media_list"),
            fromJson(dailyLogDataObject.tagsJson()),
            dailyLogDataObject.visibility(),
            Boolean.TRUE.equals(dailyLogDataObject.syncToCommunity()),
            dailyLogDataObject.communityPostId(),
            dailyLogDataObject.happenedAt(),
            dailyLogDataObject.createdAt()
        );
    }

    public DailyLogResponse toResponse(DailyLogEntity dailyLog) {
        return toResponse(dailyLog, List.of());
    }

    public DailyLogResponse toResponse(DailyLogEntity dailyLog, List<MediaAssetResponse> mediaAssets) {
        return new DailyLogResponse(
            String.valueOf(dailyLog.getDailyLogId()),
            String.valueOf(dailyLog.getPetId()),
            dailyLog.getContent(),
            dailyLog.getMediaAssetIds(),
            mediaAssets == null ? List.of() : mediaAssets,
            dailyLog.getTags(),
            dailyLog.getVisibility(),
            dailyLog.isSyncToCommunity(),
            dailyLog.getCommunityPostId() == null ? null : String.valueOf(dailyLog.getCommunityPostId()),
            DateTimeConverters.toOffsetDateTime(dailyLog.getHappenedAt()),
            DateTimeConverters.toOffsetDateTime(dailyLog.getCreatedAt())
        );
    }

    /**
     * 标签以 JSON 数组落库，保证数据库与前端之间只有一处序列化规则。
     */
    public String toTagsJson(List<String> tags) {
        return toStringListJson(tags, "萌宠日常标签格式不合法");
    }

    public String toMediaAssetIdsJson(List<String> mediaAssetIds) {
        return toStringListJson(mediaAssetIds, "萌宠日常媒体信息不合法");
    }

    private String toStringListJson(List<String> values, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, errorMessage);
        }
    }

    private List<String> fromJson(String tagsJson) {
        return fromJson(tagsJson, "pet_daily_logs.scene_tags");
    }

    private List<String> fromJson(String tagsJson, String columnName) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }

        try {
            return List.copyOf(objectMapper.readValue(tagsJson, STRING_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(columnName + " 数据格式不合法", ex);
        }
    }
}
