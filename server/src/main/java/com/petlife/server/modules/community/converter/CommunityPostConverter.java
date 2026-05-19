package com.petlife.server.modules.community.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.community.domain.entity.CommunityAuthorEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPetEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPostEntity;
import com.petlife.server.modules.community.domain.entity.CommunityTopicEntity;
import com.petlife.server.modules.community.dto.response.CommunityAuthorResponse;
import com.petlife.server.modules.community.dto.response.CommunityPetResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityTopicResponse;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import com.petlife.server.modules.community.persistence.dataobject.CommunityPostDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityTopicDataObject;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 社区帖子实体转换器。
 */
@Component
public class CommunityPostConverter {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CommunityPostConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CommunityPostEntity toEntity(CommunityPostDataObject communityPostDataObject) {
        if (communityPostDataObject == null) {
            return null;
        }

        CommunityAuthorEntity author = new CommunityAuthorEntity(
            communityPostDataObject.authorUserId(),
            communityPostDataObject.authorNickname(),
            communityPostDataObject.authorAvatarUrl()
        );
        CommunityPetEntity pet = communityPostDataObject.petId() == null
            ? null
            : new CommunityPetEntity(
                communityPostDataObject.petId(),
                communityPostDataObject.petName(),
                communityPostDataObject.petType(),
                communityPostDataObject.petBreed()
            );
        CommunityTopicEntity topic = communityPostDataObject.topicId() == null
            ? null
            : new CommunityTopicEntity(
                communityPostDataObject.topicId(),
                communityPostDataObject.topicName(),
                communityPostDataObject.topicDesc(),
                null,
                null,
                null,
                null
            );

        return new CommunityPostEntity(
            communityPostDataObject.postId(),
            communityPostDataObject.postType(),
            communityPostDataObject.title(),
            communityPostDataObject.content(),
            communityPostDataObject.sourceDailyLogId(),
            topic,
            fromJson(communityPostDataObject.mediaListJson(), "community_posts.media_list"),
            communityPostDataObject.cityCode(),
            communityPostDataObject.visibility(),
            communityPostDataObject.reviewStatus(),
            communityPostDataObject.likeCount(),
            communityPostDataObject.commentCount(),
            communityPostDataObject.favoriteCount(),
            Boolean.TRUE.equals(communityPostDataObject.liked()),
            Boolean.TRUE.equals(communityPostDataObject.favorited()),
            communityPostDataObject.publishedAt(),
            communityPostDataObject.createdAt(),
            author,
            pet
        );
    }

    public CommunityPostResponse toResponse(CommunityPostEntity communityPost) {
        return toResponse(communityPost, List.of());
    }

    public CommunityPostResponse toResponse(
        CommunityPostEntity communityPost,
        List<MediaAssetResponse> mediaAssets
    ) {
        CommunityAuthorResponse author = new CommunityAuthorResponse(
            String.valueOf(communityPost.getAuthor().getUserId()),
            communityPost.getAuthor().getNickname(),
            communityPost.getAuthor().getAvatarUrl()
        );
        CommunityPetResponse pet = communityPost.getPet() == null
            ? null
            : new CommunityPetResponse(
                String.valueOf(communityPost.getPet().getPetId()),
                communityPost.getPet().getPetName(),
                communityPost.getPet().getPetType(),
                communityPost.getPet().getBreed()
            );
        CommunityTopicResponse topic = toResponse(communityPost.getTopic());

        return new CommunityPostResponse(
            String.valueOf(communityPost.getPostId()),
            communityPost.getPostType(),
            communityPost.getTitle(),
            communityPost.getContent(),
            communityPost.getSourceDailyLogId() == null ? null : String.valueOf(communityPost.getSourceDailyLogId()),
            topic,
            communityPost.getMediaAssetIds(),
            mediaAssets == null ? List.of() : mediaAssets,
            communityPost.getCityCode(),
            communityPost.getVisibility(),
            communityPost.getReviewStatus(),
            communityPost.getLikeCount(),
            communityPost.getCommentCount(),
            communityPost.getFavoriteCount(),
            communityPost.isLiked(),
            communityPost.isFavorited(),
            DateTimeConverters.toOffsetDateTime(communityPost.getPublishedAt()),
            DateTimeConverters.toOffsetDateTime(communityPost.getCreatedAt()),
            author,
            pet
        );
    }

    public CommunityTopicEntity toEntity(CommunityTopicDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new CommunityTopicEntity(
            dataObject.topicId(),
            dataObject.topicName(),
            dataObject.topicDesc(),
            dataObject.cityCode(),
            dataObject.status(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public CommunityTopicResponse toResponse(CommunityTopicEntity topic) {
        if (topic == null) {
            return null;
        }
        return new CommunityTopicResponse(
            String.valueOf(topic.getTopicId()),
            topic.getTopicName(),
            topic.getTopicDesc(),
            topic.getCityCode(),
            topic.getStatus(),
            DateTimeConverters.toOffsetDateTime(topic.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(topic.getUpdatedAt())
        );
    }

    /**
     * 社区帖子媒体与日常媒体共用 JSON 数组格式，统一在转换器内收口解析规则。
     */
    public String toMediaAssetIdsJson(List<String> mediaAssetIds) {
        try {
            return objectMapper.writeValueAsString(mediaAssetIds == null ? List.of() : mediaAssetIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区帖子媒体信息不合法");
        }
    }

    private List<String> fromJson(String mediaListJson, String columnName) {
        if (mediaListJson == null || mediaListJson.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(mediaListJson, STRING_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(columnName + " 数据格式不合法", exception);
        }
    }
}
