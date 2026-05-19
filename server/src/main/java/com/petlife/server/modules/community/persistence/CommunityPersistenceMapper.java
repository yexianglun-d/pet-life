package com.petlife.server.modules.community.persistence;

import com.petlife.server.modules.community.persistence.command.CreateCommunityCommentCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityReportCommand;
import com.petlife.server.modules.community.persistence.command.CreateUserFollowCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.DeleteUserFollowCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostMetricsCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostReviewStatusCommand;
import com.petlife.server.modules.community.persistence.dataobject.CommunityCommentDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityPostDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityReportDataObject;
import com.petlife.server.modules.community.persistence.dataobject.CommunityTopicDataObject;
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

    String POST_SELECT_COLUMNS = """
          cp.id AS postId,
          cp.post_type AS postType,
          cp.title AS title,
          cp.content AS content,
          cp.source_daily_log_id AS sourceDailyLogId,
          cp.topic_id AS topicId,
          ct.topic_name AS topicName,
          ct.topic_desc AS topicDesc,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(cp.media_list, JSON_ARRAY()), '$')) AS mediaListJson,
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
        """;

    String POST_FROM = """
        FROM community_posts cp
        JOIN users u ON u.id = cp.user_id
        LEFT JOIN pets p ON p.id = cp.pet_id
        LEFT JOIN community_topics ct ON ct.id = cp.topic_id
        """;

    String VISIBLE_POST_CONDITION = """
          AND cp.deleted_at IS NULL
          AND cp.review_status = 'approved'
          AND (
            cp.visibility = 'public'
            OR cp.user_id = #{currentUserId}
            OR (
              cp.visibility = 'follower'
              AND EXISTS (
                SELECT 1
                FROM user_follows uf
                WHERE uf.follower_user_id = #{currentUserId}
                  AND uf.followed_user_id = cp.user_id
              )
            )
          )
        """;

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.deleted_at IS NULL
          AND cp.visibility = 'public'
          AND cp.review_status = 'approved'
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listRecommendedPosts(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.deleted_at IS NULL
          AND cp.review_status = 'approved'
          AND cp.visibility IN ('public', 'follower')
          AND EXISTS (
            SELECT 1
            FROM user_follows uf
            WHERE uf.follower_user_id = #{currentUserId}
              AND uf.followed_user_id = cp.user_id
          )
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listFollowingPosts(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.deleted_at IS NULL
          AND cp.visibility = 'public'
          AND cp.review_status = 'approved'
          AND (#{cityCode} IS NULL OR cp.city_code = #{cityCode})
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listCityPosts(
        @Param("currentUserId") Long currentUserId,
        @Param("cityCode") String cityCode
    );

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.post_type = 'qa'
        """ + VISIBLE_POST_CONDITION + """
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listQuestionPosts(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.id = #{postId}
        """ + VISIBLE_POST_CONDITION + """
        LIMIT 1
        """)
    CommunityPostDataObject findVisiblePostById(
        @Param("currentUserId") Long currentUserId,
        @Param("postId") Long postId
    );

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.id = #{postId}
          AND cp.deleted_at IS NULL
        LIMIT 1
        """)
    CommunityPostDataObject findPostById(
        @Param("currentUserId") Long currentUserId,
        @Param("postId") Long postId
    );

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.topic_id = #{topicId}
        """ + VISIBLE_POST_CONDITION + """
        ORDER BY cp.published_at DESC, cp.id DESC
        """)
    List<CommunityPostDataObject> listPostsByTopicId(
        @Param("currentUserId") Long currentUserId,
        @Param("topicId") Long topicId
    );

    @Select("""
        SELECT
          id AS topicId,
          topic_name AS topicName,
          topic_desc AS topicDesc,
          city_code AS cityCode,
          status AS status,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM community_topics
        WHERE id = #{topicId}
          AND status = 1
        LIMIT 1
        """)
    CommunityTopicDataObject findActiveTopicById(@Param("topicId") Long topicId);

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.deleted_at IS NULL
          AND (#{postType} IS NULL OR cp.post_type = #{postType})
          AND (#{reviewStatus} IS NULL OR cp.review_status = #{reviewStatus})
          AND (#{visibility} IS NULL OR cp.visibility = #{visibility})
          AND (#{authorUserId} IS NULL OR cp.user_id = #{authorUserId})
          AND (#{topicId} IS NULL OR cp.topic_id = #{topicId})
          AND (
            #{keyword} IS NULL
            OR cp.title LIKE CONCAT('%', #{keyword}, '%')
            OR cp.content LIKE CONCAT('%', #{keyword}, '%')
          )
        ORDER BY cp.created_at DESC, cp.id DESC
        LIMIT 200
        """)
    List<CommunityPostDataObject> listAdminPosts(
        @Param("currentUserId") Long currentUserId,
        @Param("postType") String postType,
        @Param("reviewStatus") String reviewStatus,
        @Param("visibility") String visibility,
        @Param("authorUserId") Long authorUserId,
        @Param("topicId") Long topicId,
        @Param("keyword") String keyword
    );

    @Select("""
        SELECT
        """ + POST_SELECT_COLUMNS + POST_FROM + """
        WHERE cp.id = #{postId}
          AND cp.deleted_at IS NULL
        LIMIT 1
        """)
    CommunityPostDataObject findAdminPostById(
        @Param("currentUserId") Long currentUserId,
        @Param("postId") Long postId
    );

    @Insert("""
        INSERT INTO community_posts (
          user_id, pet_id, topic_id, post_type, title, content, media_list, source_daily_log_id, city_code,
          visibility, review_status, published_at, created_at, updated_at
        ) VALUES (
          #{userId}, #{petId}, #{topicId}, #{postType}, #{title}, #{content}, #{mediaListJson}, #{sourceDailyLogId}, #{cityCode},
          #{visibility}, #{reviewStatus}, #{publishedAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCommunityPost(CreateCommunityPostCommand command);

    @Update("""
        UPDATE community_posts
        SET title = #{title},
            content = #{content},
            topic_id = #{topicId},
            media_list = #{mediaListJson},
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
        SET review_status = #{reviewStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{postId}
          AND deleted_at IS NULL
        """)
    int updatePostReviewStatus(UpdateCommunityPostReviewStatusCommand command);

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

    @Insert("""
        INSERT IGNORE INTO user_follows (
          follower_user_id, followed_user_id, created_at
        ) VALUES (
          #{followerUserId}, #{followedUserId}, CURRENT_TIMESTAMP
        )
        """)
    int insertUserFollow(CreateUserFollowCommand command);

    @Update("""
        DELETE FROM user_follows
        WHERE follower_user_id = #{followerUserId}
          AND followed_user_id = #{followedUserId}
        """)
    int deleteUserFollow(DeleteUserFollowCommand command);

    @Select("""
        SELECT EXISTS(
          SELECT 1
          FROM user_follows
          WHERE follower_user_id = #{followerUserId}
            AND followed_user_id = #{followedUserId}
        )
        """)
    boolean existsUserFollow(
        @Param("followerUserId") Long followerUserId,
        @Param("followedUserId") Long followedUserId
    );

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
