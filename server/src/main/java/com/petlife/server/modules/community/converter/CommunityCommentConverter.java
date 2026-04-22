package com.petlife.server.modules.community.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.community.domain.entity.CommunityAuthorEntity;
import com.petlife.server.modules.community.domain.entity.CommunityCommentEntity;
import com.petlife.server.modules.community.dto.response.CommunityAuthorResponse;
import com.petlife.server.modules.community.dto.response.CommunityCommentResponse;
import com.petlife.server.modules.community.persistence.dataobject.CommunityCommentDataObject;
import org.springframework.stereotype.Component;

/**
 * 社区评论实体转换器。
 */
@Component
public class CommunityCommentConverter {

    public CommunityCommentEntity toEntity(CommunityCommentDataObject communityCommentDataObject) {
        if (communityCommentDataObject == null) {
            return null;
        }

        CommunityAuthorEntity author = new CommunityAuthorEntity(
            communityCommentDataObject.authorUserId(),
            communityCommentDataObject.authorNickname(),
            communityCommentDataObject.authorAvatarUrl()
        );
        return new CommunityCommentEntity(
            communityCommentDataObject.commentId(),
            communityCommentDataObject.postId(),
            communityCommentDataObject.content(),
            communityCommentDataObject.createdAt(),
            author
        );
    }

    public CommunityCommentResponse toResponse(CommunityCommentEntity communityComment) {
        CommunityAuthorResponse author = new CommunityAuthorResponse(
            String.valueOf(communityComment.getAuthor().getUserId()),
            communityComment.getAuthor().getNickname(),
            communityComment.getAuthor().getAvatarUrl()
        );
        return new CommunityCommentResponse(
            String.valueOf(communityComment.getCommentId()),
            String.valueOf(communityComment.getPostId()),
            communityComment.getContent(),
            DateTimeConverters.toOffsetDateTime(communityComment.getCreatedAt()),
            author
        );
    }
}
