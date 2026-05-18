package com.petlife.server.modules.dailylog.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.modules.admin.converter.AdminContextConverter;
import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;
import com.petlife.server.modules.dailylog.domain.entity.AdminDailyLogEntity;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.dto.response.AdminDailyLogResponse;
import com.petlife.server.modules.dailylog.persistence.dataobject.AdminDailyLogDataObject;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 后台萌宠日常转换器。
 */
@Component
public class AdminDailyLogConverter {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final DailyLogEntityConverter dailyLogEntityConverter;
    private final AdminContextConverter adminContextConverter;

    public AdminDailyLogConverter(
        ObjectMapper objectMapper,
        DailyLogEntityConverter dailyLogEntityConverter,
        AdminContextConverter adminContextConverter
    ) {
        this.objectMapper = objectMapper;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
        this.adminContextConverter = adminContextConverter;
    }

    public AdminDailyLogEntity toEntity(AdminDailyLogDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        DailyLogEntity dailyLog = new DailyLogEntity(
            dataObject.dailyLogId(),
            dataObject.petId(),
            dataObject.authorUserId(),
            dataObject.content(),
            parseStringList(dataObject.mediaListJson()),
            parseStringList(dataObject.tagsJson()),
            dataObject.visibility(),
            Boolean.TRUE.equals(dataObject.syncToCommunity()),
            dataObject.communityPostId(),
            dataObject.happenedAt(),
            dataObject.createdAt()
        );
        return new AdminDailyLogEntity(
            dailyLog,
            new AdminPetContextEntity(
                dataObject.petId(),
                dataObject.petName(),
                dataObject.petType(),
                dataObject.familyId(),
                dataObject.familyName(),
                dataObject.ownerUserId(),
                dataObject.ownerNickname(),
                dataObject.ownerMobile()
            ),
            new AdminUserContextEntity(
                dataObject.authorUserId(),
                dataObject.authorNickname(),
                dataObject.authorMobile()
            )
        );
    }

    public AdminDailyLogResponse toResponse(AdminDailyLogEntity entity, List<MediaAssetResponse> mediaAssets) {
        return new AdminDailyLogResponse(
            dailyLogEntityConverter.toResponse(entity.getDailyLog(), mediaAssets),
            adminContextConverter.toPetResponse(entity.getPetContext()),
            adminContextConverter.toUserResponse(entity.getAuthorContext())
        );
    }

    private List<String> parseStringList(String valueJson) {
        if (valueJson == null || valueJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(valueJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
