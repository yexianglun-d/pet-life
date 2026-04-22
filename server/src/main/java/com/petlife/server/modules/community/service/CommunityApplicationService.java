package com.petlife.server.modules.community.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.community.converter.CommunityCommentConverter;
import com.petlife.server.modules.community.converter.CommunityPostConverter;
import com.petlife.server.modules.community.converter.CommunityReportConverter;
import com.petlife.server.modules.community.domain.entity.CommunityCommentEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPostEntity;
import com.petlife.server.modules.community.domain.entity.CommunityReportEntity;
import com.petlife.server.modules.community.dto.request.CreateCommunityCommentRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityReportRequest;
import com.petlife.server.modules.community.dto.response.CommunityCommentResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityReportResponse;
import com.petlife.server.modules.community.persistence.CommunityPersistenceMapper;
import com.petlife.server.modules.community.persistence.command.CreateCommunityCommentCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.CreateCommunityReportCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostFavoriteCommand;
import com.petlife.server.modules.community.persistence.command.DeleteCommunityPostReactionCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostCommand;
import com.petlife.server.modules.community.persistence.command.UpdateCommunityPostMetricsCommand;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社区应用服务。
 *
 * <p>当前阶段社区主链路先承接“公开萌宠日常”的分发与浏览。
 * 社区帖子由源业务驱动生成，后续再叠加关注流、审核处理等扩展能力。</p>
 */
@Service
public class CommunityApplicationService {

    private static final String TAB_RECOMMENDED = "recommended";
    private static final String POST_TYPE_EXPERIENCE = "experience";
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String REVIEW_STATUS_APPROVED = "approved";
    private static final String REACTION_TYPE_LIKE = "like";
    private static final String REPORT_TARGET_TYPE_POST = "post";
    private static final Set<String> SUPPORTED_REPORT_REASONS = Set.of(
        "spam",
        "pornography",
        "harassment",
        "illegal",
        "other"
    );

    private final CommunityPersistenceMapper communityPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final PetEntityConverter petEntityConverter;
    private final UserPersistenceMapper userPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final CommunityPostConverter communityPostConverter;
    private final CommunityCommentConverter communityCommentConverter;
    private final CommunityReportConverter communityReportConverter;

    public CommunityApplicationService(
        CommunityPersistenceMapper communityPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        PetEntityConverter petEntityConverter,
        UserPersistenceMapper userPersistenceMapper,
        UserEntityConverter userEntityConverter,
        CommunityPostConverter communityPostConverter,
        CommunityCommentConverter communityCommentConverter,
        CommunityReportConverter communityReportConverter
    ) {
        this.communityPersistenceMapper = communityPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.petEntityConverter = petEntityConverter;
        this.userPersistenceMapper = userPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.communityPostConverter = communityPostConverter;
        this.communityCommentConverter = communityCommentConverter;
        this.communityReportConverter = communityReportConverter;
    }

    public List<CommunityPostResponse> listFeed(String tab) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedTab = normalizeTab(tab);
        if (!TAB_RECOMMENDED.equals(normalizedTab)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前仅支持推荐流");
        }
        return communityPersistenceMapper.listRecommendedPosts(currentUserId).stream()
            .map(communityPostConverter::toEntity)
            .map(communityPostConverter::toResponse)
            .toList();
    }

    public CommunityPostResponse getPost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        return communityPostConverter.toResponse(requireVisiblePost(currentUserId, postId));
    }

    public List<CommunityCommentResponse> listComments(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        return communityPersistenceMapper.listCommentsByPostId(postId).stream()
            .map(communityCommentConverter::toEntity)
            .map(communityCommentConverter::toResponse)
            .toList();
    }

    @Transactional
    public CommunityCommentResponse createComment(Long postId, CreateCommunityCommentRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        CreateCommunityCommentCommand command = new CreateCommunityCommentCommand();
        command.setPostId(postId);
        command.setUserId(currentUserId);
        command.setContent(normalizeCommentContent(request.content()));
        communityPersistenceMapper.insertComment(command);
        updatePostMetrics(postId, null, 1, null);
        CommunityCommentEntity communityComment = communityCommentConverter.toEntity(
            communityPersistenceMapper.findCommentById(command.getId())
        );
        if (communityComment == null) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "评论创建后未成功回读");
        }
        return communityCommentConverter.toResponse(communityComment);
    }

    @Transactional
    public CommunityPostResponse likePost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        CreateCommunityPostReactionCommand command = new CreateCommunityPostReactionCommand();
        command.setPostId(postId);
        command.setUserId(currentUserId);
        command.setReactionType(REACTION_TYPE_LIKE);
        int insertedRows = communityPersistenceMapper.insertReaction(command);
        if (insertedRows > 0) {
            updatePostMetrics(postId, 1, null, null);
        }
        return loadVisiblePostResponse(currentUserId, postId);
    }

    @Transactional
    public CommunityPostResponse unlikePost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        DeleteCommunityPostReactionCommand command = new DeleteCommunityPostReactionCommand();
        command.setPostId(postId);
        command.setUserId(currentUserId);
        command.setReactionType(REACTION_TYPE_LIKE);
        int deletedRows = communityPersistenceMapper.deleteReaction(command);
        if (deletedRows > 0) {
            updatePostMetrics(postId, -1, null, null);
        }
        return loadVisiblePostResponse(currentUserId, postId);
    }

    @Transactional
    public CommunityPostResponse favoritePost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        CreateCommunityPostFavoriteCommand command = new CreateCommunityPostFavoriteCommand();
        command.setPostId(postId);
        command.setUserId(currentUserId);
        int insertedRows = communityPersistenceMapper.insertFavorite(command);
        if (insertedRows > 0) {
            updatePostMetrics(postId, null, null, 1);
        }
        return loadVisiblePostResponse(currentUserId, postId);
    }

    @Transactional
    public CommunityPostResponse unfavoritePost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireVisiblePost(currentUserId, postId);
        DeleteCommunityPostFavoriteCommand command = new DeleteCommunityPostFavoriteCommand();
        command.setPostId(postId);
        command.setUserId(currentUserId);
        int deletedRows = communityPersistenceMapper.deleteFavorite(command);
        if (deletedRows > 0) {
            updatePostMetrics(postId, null, null, -1);
        }
        return loadVisiblePostResponse(currentUserId, postId);
    }

    @Transactional
    public CommunityReportResponse reportPost(Long postId, CreateCommunityReportRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        CommunityPostEntity communityPost = requireVisiblePost(currentUserId, postId);
        if (communityPost.getAuthor().getUserId().equals(currentUserId)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不能举报自己的内容");
        }

        CommunityReportEntity existingPendingReport = communityReportConverter.toEntity(
            communityPersistenceMapper.findPendingReport(currentUserId, REPORT_TARGET_TYPE_POST, postId)
        );
        if (existingPendingReport != null) {
            return communityReportConverter.toResponse(existingPendingReport);
        }

        CreateCommunityReportCommand command = new CreateCommunityReportCommand();
        command.setReporterUserId(currentUserId);
        command.setTargetType(REPORT_TARGET_TYPE_POST);
        command.setTargetId(communityPost.getPostId());
        command.setReasonCode(normalizeReportReasonCode(request.reasonCode()));
        command.setReasonDetail(normalizeReportReasonDetail(command.getReasonCode(), request.reasonDetail()));
        communityPersistenceMapper.insertReport(command);

        CommunityReportEntity createdReport = communityReportConverter.toEntity(
            communityPersistenceMapper.findReportById(command.getId())
        );
        if (createdReport == null) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "举报创建后未成功回读");
        }
        return communityReportConverter.toResponse(createdReport);
    }

    @Transactional
    public Long publishDailyLog(DailyLogEntity dailyLog) {
        UserProfileEntity author = userEntityConverter.toEntity(
            userPersistenceMapper.findUserProfileById(dailyLog.getAuthorUserId())
        );
        if (author == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "萌宠日常作者不存在");
        }

        PetProfileEntity pet = requirePet(dailyLog.getPetId());
        String title = buildPostTitle(dailyLog.getContent());

        if (dailyLog.getCommunityPostId() != null) {
            UpdateCommunityPostCommand updateCommand = new UpdateCommunityPostCommand();
            updateCommand.setPostId(dailyLog.getCommunityPostId());
            updateCommand.setTitle(title);
            updateCommand.setContent(dailyLog.getContent());
            updateCommand.setCityCode(author.getCityCode());
            updateCommand.setVisibility(VISIBILITY_PUBLIC);
            updateCommand.setReviewStatus(REVIEW_STATUS_APPROVED);
            int updatedRows = communityPersistenceMapper.updateCommunityPost(updateCommand);
            if (updatedRows > 0) {
                return dailyLog.getCommunityPostId();
            }
        }

        CreateCommunityPostCommand createCommand = new CreateCommunityPostCommand();
        createCommand.setUserId(author.getUserId());
        createCommand.setPetId(pet.getPetId());
        createCommand.setPostType(POST_TYPE_EXPERIENCE);
        createCommand.setTitle(title);
        createCommand.setContent(dailyLog.getContent());
        createCommand.setSourceDailyLogId(dailyLog.getDailyLogId());
        createCommand.setCityCode(author.getCityCode());
        createCommand.setVisibility(VISIBILITY_PUBLIC);
        /**
         * 审核模块尚未正式接入前，萌宠日常同步社区采用“系统已确认公开”的内容来源，
         * 这里直接标记为 approved，保证推荐流真实可见，而不是生成永远不可读的 pending 数据。
         */
        createCommand.setReviewStatus(REVIEW_STATUS_APPROVED);
        createCommand.setPublishedAt(LocalDateTime.now());
        communityPersistenceMapper.insertCommunityPost(createCommand);
        return createCommand.getId();
    }

    @Transactional
    public void removeDailyLogPost(Long communityPostId, Long sourceDailyLogId) {
        if (communityPostId == null) {
            return;
        }
        DeleteCommunityPostCommand command = new DeleteCommunityPostCommand();
        command.setPostId(communityPostId);
        command.setSourceDailyLogId(sourceDailyLogId);
        communityPersistenceMapper.deleteCommunityPost(command);
    }

    private CommunityPostEntity requireVisiblePost(Long currentUserId, Long postId) {
        CommunityPostEntity communityPost = communityPostConverter.toEntity(
            communityPersistenceMapper.findVisiblePostById(currentUserId, postId)
        );
        if (communityPost == null) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
        return communityPost;
    }

    private CommunityPostResponse loadVisiblePostResponse(Long currentUserId, Long postId) {
        return communityPostConverter.toResponse(requireVisiblePost(currentUserId, postId));
    }

    private PetProfileEntity requirePet(Long petId) {
        PetProfileEntity pet = petEntityConverter.toEntity(petPersistenceMapper.findPetById(petId));
        if (pet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return pet;
    }

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return TAB_RECOMMENDED;
        }
        return tab.trim();
    }

    private String buildPostTitle(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.length() <= 24) {
            return normalizedContent;
        }
        return normalizedContent.substring(0, 24) + "...";
    }

    /**
     * 评论是社区主链路里的公共互动能力，先把空白和超长文本在应用层收口，
     * 避免数据库截断后前端和审核侧看到不一致内容。
     */
    private String normalizeCommentContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "评论内容不能为空");
        }
        if (normalizedContent.length() > 1000) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "评论内容不能超过 1000 字");
        }
        return normalizedContent;
    }

    private void updatePostMetrics(Long postId, Integer likeDelta, Integer commentDelta, Integer favoriteDelta) {
        UpdateCommunityPostMetricsCommand command = new UpdateCommunityPostMetricsCommand();
        command.setPostId(postId);
        command.setLikeDelta(likeDelta);
        command.setCommentDelta(commentDelta);
        command.setFavoriteDelta(favoriteDelta);
        communityPersistenceMapper.updatePostMetrics(command);
    }

    private String normalizeReportReasonCode(String reasonCode) {
        String normalizedReasonCode = reasonCode == null ? "" : reasonCode.trim().toLowerCase();
        if (!SUPPORTED_REPORT_REASONS.contains(normalizedReasonCode)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "举报原因不支持");
        }
        return normalizedReasonCode;
    }

    /**
     * 举报补充说明直接进入后台处理队列，这里统一做长度与必填约束，
     * 避免“其他原因”进入审核台后没有任何可判断信息。
     */
    private String normalizeReportReasonDetail(String reasonCode, String reasonDetail) {
        String normalizedReasonDetail = reasonDetail == null ? null : reasonDetail.trim();
        if (normalizedReasonDetail != null && normalizedReasonDetail.isEmpty()) {
            normalizedReasonDetail = null;
        }
        if ("other".equals(reasonCode) && normalizedReasonDetail == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "其他原因必须补充说明");
        }
        if (normalizedReasonDetail != null && normalizedReasonDetail.length() > 500) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "举报补充说明不能超过 500 字");
        }
        return normalizedReasonDetail;
    }
}
