package com.petlife.server.modules.community.persistence;

import com.petlife.server.modules.community.persistence.command.CreateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityCommentCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityReportCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostMetricsCommand;
import com.petlife.server.modules.community.persistence.dataobject.CommunityCommentDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityPostDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityReportDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 社区帖子持久化 Mapper。
 */
@Mapper
public interface CommunityPersistenceMapper {

    @Select("""
        SELECT
          cp.id AS postId,
          cp.post_type AS postType,
          cp.title AS title,
          cp.content AS content,
          cp.source_daily_log_id AS sourceDailyLogId,
          cp.city_code AS cityCode,
          cp.visibility AS visibility,
          cp.review_status AS reviewStatus,
          cp.like_count AS likeCount,
          cp.comment_count AS commentCount,
          cp.favorite_count AS favoriteCount,
          EXISTS (
            SELECT 1
            FROM community_post_reactions cpr
            WHERE cpr.post_id = cp.id
              AND cpr.user_id = #{currentUserId}
              AND cpr.reaction_type = 'like'
          ) AS liked,
          EXISTS (
            SELECT 1
            FROM community_post_favorites cpf
            WHERE cpf.post_id = cp.id
              AND cpf.user_id = #{currentUserId}
          ) AS favorited,
          cp.published_at AS publishedAt,
          cp.created_at AS createdAt,
          u.id AS authorUserId,
          u.nickname AS authorNickname,
          u.avatar_url AS authorAvatarUrl,
          p.id AS petId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS petBreed
        FROM community_posts cp
        JOIN users u ON u.id = cp.user_id
        LEFT JOIN pets p ON p.id = cp.pet_id
        WHERE cp.deleted_at IS NULL
          AND cp.visibility = 'public'
          AND cp.review_status = 'approved'
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listRecommendedPosts(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
          cp.id AS postId,
          cp.post_type AS postType,
          cp.title AS title,
          cp.content AS content,
          cp.source_daily_log_id AS sourceDailyLogId,
          cp.city_code AS cityCode,
          cp.visibility AS visibility,
          cp.review_status AS reviewStatus,
          cp.like_count AS likeCount,
          cp.comment_count AS commentCount,
          cp.favorite_count AS favoriteCount,
          EXISTS (
            SELECT 1
            FROM community_post_reactions cpr
            WHERE cpr.post_id = cp.id
              AND cpr.user_id = #{currentUserId}
              AND cpr.reaction_type = 'like'
          ) AS liked,
          EXISTS (
            SELECT 1
            FROM community_post_favorites cpf
            WHERE cpf.post_id = cp.id
              AND cpf.user_id = #{currentUserId}
          ) AS favorited,
          cp.published_at AS publishedAt,
          cp.created_at AS createdAt,
          u.id AS authorUserId,
          u.nickname AS authorNickname,
          u.avatar_url AS authorAvatarUrl,
          p.id AS petId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS petBreed
        FROM community_posts cp
        JOIN users u ON u.id = cp.user_id
        LEFT JOIN pets p ON p.id = cp.pet_id
        WHERE cp.id = #{postId}
          AND cp.deleted_at IS NULL
          AND cp.visibility = 'public'
          AND cp.review_status = 'approved'
        LIMIT 1
        """)
    CommunityPostDataObject findVisiblePostById(
        @Param("currentUserId") Long currentUserId,
        @Param("postId") Long postId
    );

    @Select("""
        SELECT
          cp.id AS postId,
          cp.post_type AS postType,
          cp.title AS title,
          cp.content AS content,
          cp.source_daily_log_id AS sourceDailyLogId,
          cp.city_code AS cityCode,
          cp.visibility AS visibility,
          cp.review_status AS reviewStatus,
          cp.like_count AS likeCount,
          cp.comment_count AS commentCount,
          cp.favorite_count AS favoriteCount,
          EXISTS (
            SELECT 1
            FROM community_post_reactions cpr
            WHERE cpr.post_id = cp.id
              AND cpr.user_id = #{currentUserId}
              AND cpr.reaction_type = 'like'
          ) AS liked,
          EXISTS (
            SELECT 1
            FROM community_post_favorites cpf
            WHERE cpf.post_id = cp.id
              AND cpf.user_id = #{currentUserId}
          ) AS favorited,
          cp.published_at AS publishedAt,
          cp.created_at AS createdAt,
          u.id AS authorUserId,
          u.nickname AS authorNickname,
          u.avatar_url AS authorAvatarUrl,
          p.id AS petId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS petBreed
        FROM community_posts cp
        JOIN users u ON u.id = cp.user_id
        LEFT JOIN pets p ON p.id = cp.pet_id
        WHERE cp.id = #{postId}
          AND cp.deleted_at IS NULL
        LIMIT 1
        """)
    CommunityPostDataObject findPostById(
        @Param("currentUserId") Long currentUserId,
        @Param("postId") Long postId
    );

    @Insert("""
        INSERT INTO community_posts (
          user_id, pet_id, post_type, title, content, source_daily_log_id, city_code,
          visibility, review_status, published_at, created_at, updated_at
        ) VALUES (
          #{userId}, #{petId}, #{postType}, #{title}, #{content}, #{sourceDailyLogId}, #{cityCode},
          #{visibility}, #{reviewStatus}, #{publishedAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCommunityPost(CreateCommunityPostCommand command);

    @Select("""
        SELECT
          cc.id AS commentId,
          cc.post_id AS postId,
          cc.content AS content,
          cc.created_at AS createdAt,
          u.id AS authorUserId,
          u.nickname AS authorNickname,
          u.avatar_url AS authorAvatarUrl
        FROM community_comments cc
        JOIN users u ON u.id = cc.user_id
        WHERE cc.post_id = #{postId}
          AND cc.deleted_at IS NULL
          AND cc.status = 'normal'
        ORDER BY cc.created_at ASC, cc.id ASC
        """)
    List<CommunityCommentDataObject> listCommentsByPostId(@Param("postId") Long postId);

    @Select("""
        SELECT
          cc.id AS commentId,
          cc.post_id AS postId,
          cc.content AS content,
          cc.created_at AS createdAt,
          u.id AS authorUserId,
          u.nickname AS authorNickname,
          u.avatar_url AS authorAvatarUrl
        FROM community_comments cc
        JOIN users u ON u.id = cc.user_id
        WHERE cc.id = #{commentId}
          AND cc.deleted_at IS NULL
          AND cc.status = 'normal'
        LIMIT 1
        """)
    CommunityCommentDataObject findCommentById(@Param("commentId") Long commentId);

    @Insert("""
        INSERT INTO community_comments (
          post_id, user_id, parent_comment_id, content, status, created_at, updated_at
        ) VALUES (
          #{postId}, #{userId}, NULL, #{content}, 'normal', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(CreateCommunityCommentCommand command);

    @Update("""
        UPDATE community_posts
        SET title = #{title},
            content = #{content},
            city_code = #{cityCode},
            visibility = #{visibility},
            review_status = #{reviewStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{postId}
          AND deleted_at IS NULL
        """)
    int updateCommunityPost(UpdateCommunityPostCommand command);

    @Update("""
        UPDATE community_posts
        SET like_count = GREATEST(like_count + COALESCE(#{likeDelta}, 0), 0),
            comment_count = GREATEST(comment_count + COALESCE(#{commentDelta}, 0), 0),
            favorite_count = GREATEST(favorite_count + COALESCE(#{favoriteDelta}, 0), 0),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{postId}
          AND deleted_at IS NULL
        """)
    int updatePostMetrics(UpdateCommunityPostMetricsCommand command);

    @Insert("""
        INSERT IGNORE INTO community_post_reactions (
          post_id, user_id, reaction_type, created_at
        ) VALUES (
          #{postId}, #{userId}, #{reactionType}, CURRENT_TIMESTAMP
        )
        """)
    int insertReaction(CreateCommunityPostReactionCommand command);

    @Update("""
        DELETE FROM community_post_reactions
        WHERE post_id = #{postId}
          AND user_id = #{userId}
          AND reaction_type = #{reactionType}
        """)
    int deleteReaction(DeleteCommunityPostReactionCommand command);

    @Insert("""
        INSERT IGNORE INTO community_post_favorites (
          post_id, user_id, created_at
        ) VALUES (
          #{postId}, #{userId}, CURRENT_TIMESTAMP
        )
        """)
    int insertFavorite(CreateCommunityPostFavoriteCommand command);

    @Update("""
        DELETE FROM community_post_favorites
        WHERE post_id = #{postId}
          AND user_id = #{userId}
        """)
    int deleteFavorite(DeleteCommunityPostFavoriteCommand command);

    @Update("""
        UPDATE community_posts
        SET deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{postId}
          AND source_daily_log_id = #{sourceDailyLogId}
          AND deleted_at IS NULL
        """)
    int deleteCommunityPost(DeleteCommunityPostCommand command);

    @Select("""
        SELECT
          cr.id AS reportId,
          cr.reporter_user_id AS reporterUserId,
          cr.target_type AS targetType,
          cr.target_id AS targetId,
          cr.reason_code AS reasonCode,
          cr.reason_detail AS reasonDetail,
          cr.status AS status,
          cr.created_at AS createdAt
        FROM community_reports cr
        WHERE cr.id = #{reportId}
        LIMIT 1
        """)
    CommunityReportDataObject findReportById(@Param("reportId") Long reportId);

    @Select("""
        SELECT
          cr.id AS reportId,
          cr.reporter_user_id AS reporterUserId,
          cr.target_type AS targetType,
          cr.target_id AS targetId,
          cr.reason_code AS reasonCode,
          cr.reason_detail AS reasonDetail,
          cr.status AS status,
          cr.created_at AS createdAt
        FROM community_reports cr
        WHERE cr.reporter_user_id = #{reporterUserId}
          AND cr.target_type = #{targetType}
          AND cr.target_id = #{targetId}
          AND cr.status = 'pending'
        ORDER BY cr.id DESC
        LIMIT 1
        """)
    CommunityReportDataObject findPendingReport(
        @Param("reporterUserId") Long reporterUserId,
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    @Insert("""
        INSERT INTO community_reports (
          reporter_user_id, target_type, target_id, reason_code, reason_detail, status, created_at, updated_at
        ) VALUES (
          #{reporterUserId}, #{targetType}, #{targetId}, #{reasonCode}, #{reasonDetail}, 'pending',
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(CreateCommunityReportCommand command);
}
