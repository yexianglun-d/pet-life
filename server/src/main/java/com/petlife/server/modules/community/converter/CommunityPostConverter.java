package com.petlife.server.modules.community.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.community.domain.entity.CommunityAuthorEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPetEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPostEntity;
import com.petlife.server.modules.community.dto.response.CommunityAuthorResponse;
import com.petlife.server.modules.community.dto.response.CommunityPetResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.persistence.dataobject.CommunityPostDataObject;
import org.springframework.stereotype.Component;

/**
 * 社区帖子实体转换器。
 */
@Component
public class CommunityPostConverter {

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

        return new CommunityPostEntity(
            communityPostDataObject.postId(),
            communityPostDataObject.postType(),
            communityPostDataObject.title(),
            communityPostDataObject.content(),
            communityPostDataObject.sourceDailyLogId(),
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

        return new CommunityPostResponse(
            String.valueOf(communityPost.getPostId()),
            communityPost.getPostType(),
            communityPost.getTitle(),
            communityPost.getContent(),
            communityPost.getSourceDailyLogId() == null ? null : String.valueOf(communityPost.getSourceDailyLogId()),
            communityPost.getCityCode(),
            communityPost.getVisibility(),
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
}
