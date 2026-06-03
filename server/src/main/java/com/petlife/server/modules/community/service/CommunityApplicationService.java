package com.petlife.server.modules.community.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.community.converter.CommunityCommentConverter;
import com.petlife.server.modules.community.converter.CommunityPostConverter;
import com.petlife.server.modules.community.converter.CommunityReportConverter;
import com.petlife.server.modules.community.domain.entity.CommunityCommentEntity;
import com.petlife.server.modules.community.domain.entity.CommunityPostEntity;
import com.petlife.server.modules.community.domain.entity.CommunityReportEntity;
import com.petlife.server.modules.community.domain.entity.CommunityTopicEntity;
import com.petlife.server.modules.community.dto.request.AdminUpdateCommunityContentStatusRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityCommentRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityPostRequest;
import com.petlife.server.modules.community.dto.request.CreateCommunityReportRequest;
import com.petlife.server.modules.community.dto.response.CommunityCommentResponse;
import com.petlife.server.modules.community.dto.response.CommunityFollowStatusResponse;
import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.community.dto.response.CommunityQuestionDetailResponse;
import com.petlife.server.modules.community.dto.response.CommunityReportResponse;
import com.petlife.server.modules.community.dto.response.CommunityTopicDetailResponse;
import com.petlife.server.modules.community.persistence.CommunityPersistenceMapper;
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
import com.petlife.server.modules.community.persistence.dataobject.CommunityPostDataObject;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import com.petlife.server.modules.media.service.MediaAssetApplicationService;
import com.petlife.server.modules.moderation.service.ModerationTaskApplicationService;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社区应用服务。
 *
 * <p>社区主链路同时承接公开萌宠日常同步和用户独立发布。
 * 用户侧只读取 approved 内容，后台治理通过 review_status 控制内容上下架。</p>
 */
@Service
public class CommunityApplicationService {

    private static final String TAB_RECOMMENDED = "recommended";
    private static final String TAB_FOLLOWING = "following";
    private static final String TAB_CITY = "city";
    private static final String TAB_QA = "qa";
    private static final String POST_TYPE_IMAGE_TEXT = "image_text";
    private static final String POST_TYPE_VIDEO = "video";
    private static final String POST_TYPE_QA = "qa";
    private static final String POST_TYPE_EXPERIENCE = "experience";
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String VISIBILITY_FOLLOWER = "follower";
    private static final String REVIEW_STATUS_PENDING_REVIEW = "pending_review";
    private static final String REVIEW_STATUS_APPROVED = "approved";
    private static final String REVIEW_STATUS_REJECTED = "rejected";
    private static final String REACTION_TYPE_LIKE = "like";
    private static final String REPORT_TARGET_TYPE_POST = "post";
    private static final String AUDIT_TARGET_TYPE_COMMUNITY_POST = "community_post";
    private static final String AUDIT_TARGET_TYPE_COMMUNITY_QUESTION = "community_question";
    private static final String ADMIN_ACTION_TAKE_DOWN = "take_down";
    private static final String ADMIN_ACTION_RESTORE = "restore";
    private static final String BIZ_TYPE_COMMUNITY = "community";
    private static final Set<String> SUPPORTED_FEED_TABS = Set.of(
        TAB_RECOMMENDED,
        TAB_FOLLOWING,
        TAB_CITY,
        TAB_QA
    );
    private static final Set<String> SUPPORTED_POST_TYPES = Set.of(
        POST_TYPE_IMAGE_TEXT,
        POST_TYPE_VIDEO,
        POST_TYPE_QA,
        POST_TYPE_EXPERIENCE
    );
    private static final Set<String> SUPPORTED_VISIBILITIES = Set.of(
        VISIBILITY_PUBLIC,
        VISIBILITY_FOLLOWER
    );
    private static final Set<String> SUPPORTED_ADMIN_ACTIONS = Set.of(
        ADMIN_ACTION_TAKE_DOWN,
        ADMIN_ACTION_RESTORE
    );
    private static final Set<String> SUPPORTED_REVIEW_STATUSES = Set.of(
        "pending_review",
        REVIEW_STATUS_APPROVED,
        REVIEW_STATUS_REJECTED
    );
    private static final Set<String> COMMUNITY_MEDIA_TYPES = Set.of("image", "video");
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
    private final MediaAssetApplicationService mediaAssetApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ModerationTaskApplicationService moderationTaskApplicationService;

    public CommunityApplicationService(
        CommunityPersistenceMapper communityPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        PetEntityConverter petEntityConverter,
        UserPersistenceMapper userPersistenceMapper,
        UserEntityConverter userEntityConverter,
        CommunityPostConverter communityPostConverter,
        CommunityCommentConverter communityCommentConverter,
        CommunityReportConverter communityReportConverter,
        MediaAssetApplicationService mediaAssetApplicationService,
        AuditLogApplicationService auditLogApplicationService,
        ModerationTaskApplicationService moderationTaskApplicationService
    ) {
        this.communityPersistenceMapper = communityPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.petEntityConverter = petEntityConverter;
        this.userPersistenceMapper = userPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.communityPostConverter = communityPostConverter;
        this.communityCommentConverter = communityCommentConverter;
        this.communityReportConverter = communityReportConverter;
        this.mediaAssetApplicationService = mediaAssetApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
        this.moderationTaskApplicationService = moderationTaskApplicationService;
    }

    public List<CommunityPostResponse> listFeed(String tab, String cityCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedTab = normalizeTab(tab);
        List<CommunityPostEntity> posts = switch (normalizedTab) {
            case TAB_RECOMMENDED -> toPostEntities(communityPersistenceMapper.listRecommendedPosts(currentUserId));
            case TAB_FOLLOWING -> toPostEntities(communityPersistenceMapper.listFollowingPosts(currentUserId));
            case TAB_CITY -> toPostEntities(
                communityPersistenceMapper.listCityPosts(currentUserId, resolveCityCode(currentUserId, cityCode))
            );
            case TAB_QA -> toPostEntities(communityPersistenceMapper.listQuestionPosts(currentUserId));
            default -> throw new BusinessException(ResponseCode.BAD_REQUEST, "社区信息流类型不支持");
        };
        return toPostResponses(posts);
    }

    @Transactional
    public CommunityPostResponse createPost(CreateCommunityPostRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfileEntity author = requireUser(currentUserId);
        PetProfileEntity pet = request.petId() == null ? null : requireAccessiblePet(currentUserId, request.petId());
        CommunityTopicEntity topic = request.topicId() == null ? null : requireActiveTopic(request.topicId());
        List<String> mediaAssetIds = mediaAssetApplicationService.validateUsableAssetIds(
            currentUserId,
            request.mediaAssetIds(),
            BIZ_TYPE_COMMUNITY,
            COMMUNITY_MEDIA_TYPES
        );

        CreateCommunityPostCommand command = new CreateCommunityPostCommand();
        command.setUserId(currentUserId);
        command.setPetId(pet == null ? null : pet.getPetId());
        command.setTopicId(topic == null ? null : topic.getTopicId());
        command.setPostType(normalizePostType(request.postType()));
        command.setContent(normalizePostContent(request.content()));
        command.setTitle(normalizePostTitle(request.title(), command.getContent(), command.getPostType()));
        command.setMediaListJson(communityPostConverter.toMediaAssetIdsJson(mediaAssetIds));
        command.setSourceDailyLogId(null);
        command.setCityCode(resolvePostCityCode(author, request.cityCode()));
        command.setVisibility(normalizeVisibility(request.visibility()));
        command.setReviewStatus(REVIEW_STATUS_PENDING_REVIEW);
        command.setPublishedAt(LocalDateTime.now());
        communityPersistenceMapper.insertCommunityPost(command);
        CommunityPostEntity createdPost = requireExistingPost(currentUserId, command.getId());
        createModerationTaskForPost(createdPost);
        return toPostResponse(createdPost);
    }

    public List<CommunityPostResponse> listMyPosts(String reviewStatus) {
        Long currentUserId = CurrentUserContext.requireUserId();
        return toPostResponses(toPostEntities(communityPersistenceMapper.listMyPosts(
            currentUserId,
            normalizeNullableReviewStatus(reviewStatus)
        )));
    }

    @Transactional
    public CommunityPostResponse updatePost(Long postId, CreateCommunityPostRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfileEntity author = requireUser(currentUserId);
        CommunityPostEntity existingPost = requireEditableOwnPost(currentUserId, postId);
        PetProfileEntity pet = request.petId() == null ? null : requireAccessiblePet(currentUserId, request.petId());
        CommunityTopicEntity topic = request.topicId() == null ? null : requireActiveTopic(request.topicId());
        List<String> mediaAssetIds = mediaAssetApplicationService.validateUsableAssetIds(
            currentUserId,
            request.mediaAssetIds(),
            BIZ_TYPE_COMMUNITY,
            COMMUNITY_MEDIA_TYPES
        );

        UpdateCommunityPostCommand command = new UpdateCommunityPostCommand();
        command.setPostId(existingPost.getPostId());
        command.setUserId(currentUserId);
        command.setPetId(pet == null ? null : pet.getPetId());
        String normalizedPostType = normalizePostType(request.postType());
        String normalizedContent = normalizePostContent(request.content());
        command.setPostType(normalizedPostType);
        command.setTitle(normalizePostTitle(request.title(), normalizedContent, normalizedPostType));
        command.setContent(normalizedContent);
        command.setTopicId(topic == null ? null : topic.getTopicId());
        command.setMediaListJson(communityPostConverter.toMediaAssetIdsJson(mediaAssetIds));
        command.setCityCode(resolvePostCityCode(author, request.cityCode()));
        command.setVisibility(normalizeVisibility(request.visibility()));
        command.setReviewStatus(REVIEW_STATUS_PENDING_REVIEW);
        int updatedRows = communityPersistenceMapper.updateCommunityPost(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }

        CommunityPostEntity updatedPost = requireExistingPost(currentUserId, existingPost.getPostId());
        createModerationTaskForPost(updatedPost);
        return toPostResponse(updatedPost);
    }

    public CommunityPostResponse getPost(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        return toPostResponse(requireVisiblePost(currentUserId, postId));
    }

    public CommunityTopicDetailResponse getTopic(Long topicId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        CommunityTopicEntity topic = requireActiveTopic(topicId);
        List<CommunityPostResponse> posts = toPostResponses(
            toPostEntities(communityPersistenceMapper.listPostsByTopicId(currentUserId, topic.getTopicId()))
        );
        return new CommunityTopicDetailResponse(communityPostConverter.toResponse(topic), posts);
    }

    public CommunityQuestionDetailResponse getQuestion(Long questionId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        CommunityPostEntity question = requireVisiblePost(currentUserId, questionId);
        if (!POST_TYPE_QA.equals(question.getPostType())) {
            throw new BusinessException(ResponseCode.COMMUNITY_QUESTION_NOT_FOUND);
        }
        List<CommunityCommentResponse> answers = communityPersistenceMapper.listCommentsByPostId(questionId).stream()
            .map(communityCommentConverter::toEntity)
            .map(communityCommentConverter::toResponse)
            .toList();
        return new CommunityQuestionDetailResponse(toPostResponse(question), answers);
    }

    public List<CommunityCommentResponse> listComments(Long postId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireApprovedVisiblePost(currentUserId, postId);
        return communityPersistenceMapper.listCommentsByPostId(postId).stream()
            .map(communityCommentConverter::toEntity)
            .map(communityCommentConverter::toResponse)
            .toList();
    }

    @Transactional
    public CommunityCommentResponse createComment(Long postId, CreateCommunityCommentRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireApprovedVisiblePost(currentUserId, postId);
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
        requireApprovedVisiblePost(currentUserId, postId);
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
        requireApprovedVisiblePost(currentUserId, postId);
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
        requireApprovedVisiblePost(currentUserId, postId);
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
        requireApprovedVisiblePost(currentUserId, postId);
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
    public CommunityFollowStatusResponse followUser(Long followedUserId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireFollowTarget(currentUserId, followedUserId);
        CreateUserFollowCommand command = new CreateUserFollowCommand();
        command.setFollowerUserId(currentUserId);
        command.setFollowedUserId(followedUserId);
        communityPersistenceMapper.insertUserFollow(command);
        return new CommunityFollowStatusResponse(String.valueOf(followedUserId), true);
    }

    @Transactional
    public CommunityFollowStatusResponse unfollowUser(Long followedUserId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireFollowTarget(currentUserId, followedUserId);
        DeleteUserFollowCommand command = new DeleteUserFollowCommand();
        command.setFollowerUserId(currentUserId);
        command.setFollowedUserId(followedUserId);
        communityPersistenceMapper.deleteUserFollow(command);
        return new CommunityFollowStatusResponse(String.valueOf(followedUserId), false);
    }

    public CommunityFollowStatusResponse getFollowStatus(Long followedUserId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireFollowTarget(currentUserId, followedUserId);
        return new CommunityFollowStatusResponse(
            String.valueOf(followedUserId),
            communityPersistenceMapper.existsUserFollow(currentUserId, followedUserId)
        );
    }

    @Transactional
    public CommunityReportResponse reportPost(Long postId, CreateCommunityReportRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        CommunityPostEntity communityPost = requireApprovedVisiblePost(currentUserId, postId);
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

    public List<CommunityPostResponse> listAdminPosts(
        String postType,
        String reviewStatus,
        String visibility,
        Long authorUserId,
        Long topicId,
        String keyword
    ) {
        return toPostResponses(toPostEntities(communityPersistenceMapper.listAdminPosts(
            null,
            normalizeNullablePostType(postType),
            normalizeNullableReviewStatus(reviewStatus),
            normalizeNullableVisibility(visibility),
            authorUserId,
            topicId,
            normalizeNullableText(keyword, 100)
        )));
    }

    public CommunityPostResponse getAdminPost(Long postId) {
        return toPostResponse(requireAdminPost(postId));
    }

    @Transactional
    public CommunityPostResponse updateAdminPostStatus(
        Long postId,
        AdminOperationContext operationContext,
        AdminUpdateCommunityContentStatusRequest request
    ) {
        return updateAdminContentStatus(
            postId,
            null,
            AUDIT_TARGET_TYPE_COMMUNITY_POST,
            operationContext,
            request
        );
    }

    public List<CommunityPostResponse> listAdminQuestions(
        String reviewStatus,
        String visibility,
        Long authorUserId,
        Long topicId,
        String keyword
    ) {
        return toPostResponses(toPostEntities(communityPersistenceMapper.listAdminPosts(
            null,
            POST_TYPE_QA,
            normalizeNullableReviewStatus(reviewStatus),
            normalizeNullableVisibility(visibility),
            authorUserId,
            topicId,
            normalizeNullableText(keyword, 100)
        )));
    }

    public CommunityQuestionDetailResponse getAdminQuestion(Long questionId) {
        CommunityPostEntity question = requireAdminPost(questionId);
        requirePostType(question, POST_TYPE_QA, ResponseCode.COMMUNITY_QUESTION_NOT_FOUND);
        List<CommunityCommentResponse> answers = communityPersistenceMapper.listCommentsByPostId(questionId).stream()
            .map(communityCommentConverter::toEntity)
            .map(communityCommentConverter::toResponse)
            .toList();
        return new CommunityQuestionDetailResponse(toPostResponse(question), answers);
    }

    @Transactional
    public CommunityPostResponse updateAdminQuestionStatus(
        Long questionId,
        AdminOperationContext operationContext,
        AdminUpdateCommunityContentStatusRequest request
    ) {
        return updateAdminContentStatus(
            questionId,
            POST_TYPE_QA,
            AUDIT_TARGET_TYPE_COMMUNITY_QUESTION,
            operationContext,
            request
        );
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
            updateCommand.setPetId(pet.getPetId());
            updateCommand.setPostType(POST_TYPE_EXPERIENCE);
            updateCommand.setTitle(title);
            updateCommand.setContent(dailyLog.getContent());
            updateCommand.setTopicId(null);
            updateCommand.setMediaListJson(communityPostConverter.toMediaAssetIdsJson(dailyLog.getMediaAssetIds()));
            updateCommand.setCityCode(author.getCityCode());
            updateCommand.setVisibility(VISIBILITY_PUBLIC);
            updateCommand.setReviewStatus(REVIEW_STATUS_PENDING_REVIEW);
            int updatedRows = communityPersistenceMapper.updateCommunityPost(updateCommand);
            if (updatedRows > 0) {
                CommunityPostEntity updatedPost = requireExistingPost(author.getUserId(), dailyLog.getCommunityPostId());
                createModerationTaskForPost(updatedPost);
                return dailyLog.getCommunityPostId();
            }
        }

        CreateCommunityPostCommand createCommand = new CreateCommunityPostCommand();
        createCommand.setUserId(author.getUserId());
        createCommand.setPetId(pet.getPetId());
        createCommand.setPostType(POST_TYPE_EXPERIENCE);
        createCommand.setTitle(title);
        createCommand.setContent(dailyLog.getContent());
        createCommand.setTopicId(null);
        createCommand.setMediaListJson(communityPostConverter.toMediaAssetIdsJson(dailyLog.getMediaAssetIds()));
        createCommand.setSourceDailyLogId(dailyLog.getDailyLogId());
        createCommand.setCityCode(author.getCityCode());
        createCommand.setVisibility(VISIBILITY_PUBLIC);
        createCommand.setReviewStatus(REVIEW_STATUS_PENDING_REVIEW);
        createCommand.setPublishedAt(LocalDateTime.now());
        communityPersistenceMapper.insertCommunityPost(createCommand);
        CommunityPostEntity createdPost = requireExistingPost(author.getUserId(), createCommand.getId());
        createModerationTaskForPost(createdPost);
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

    private CommunityPostEntity requireApprovedVisiblePost(Long currentUserId, Long postId) {
        CommunityPostEntity communityPost = requireVisiblePost(currentUserId, postId);
        if (!REVIEW_STATUS_APPROVED.equals(communityPost.getReviewStatus())) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
        return communityPost;
    }

    private CommunityPostResponse loadVisiblePostResponse(Long currentUserId, Long postId) {
        return toPostResponse(requireVisiblePost(currentUserId, postId));
    }

    private CommunityPostEntity requireExistingPost(Long currentUserId, Long postId) {
        CommunityPostEntity communityPost = communityPostConverter.toEntity(
            communityPersistenceMapper.findPostById(currentUserId, postId)
        );
        if (communityPost == null) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
        return communityPost;
    }

    private CommunityPostEntity requireEditableOwnPost(Long currentUserId, Long postId) {
        CommunityPostEntity communityPost = requireExistingPost(currentUserId, postId);
        if (!currentUserId.equals(communityPost.getAuthor().getUserId())) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
        if (REVIEW_STATUS_APPROVED.equals(communityPost.getReviewStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "已公开内容暂不支持直接编辑，请先下架后重新提交");
        }
        return communityPost;
    }

    private CommunityPostEntity requireAdminPost(Long postId) {
        CommunityPostEntity communityPost = communityPostConverter.toEntity(
            communityPersistenceMapper.findAdminPostById(null, postId)
        );
        if (communityPost == null) {
            throw new BusinessException(ResponseCode.COMMUNITY_POST_NOT_FOUND);
        }
        return communityPost;
    }

    private CommunityPostResponse updateAdminContentStatus(
        Long postId,
        String requiredPostType,
        String auditTargetType,
        AdminOperationContext operationContext,
        AdminUpdateCommunityContentStatusRequest request
    ) {
        CommunityPostEntity communityPost = requireAdminPost(postId);
        if (requiredPostType != null) {
            requirePostType(communityPost, requiredPostType, ResponseCode.COMMUNITY_QUESTION_NOT_FOUND);
        }
        String action = normalizeAdminAction(request.action());
        String nextReviewStatus = ADMIN_ACTION_TAKE_DOWN.equals(action)
            ? REVIEW_STATUS_REJECTED
            : REVIEW_STATUS_APPROVED;
        UpdateCommunityPostReviewStatusCommand command = new UpdateCommunityPostReviewStatusCommand();
        command.setPostId(postId);
        command.setReviewStatus(nextReviewStatus);
        int updatedRows = communityPersistenceMapper.updatePostReviewStatus(command);
        if (updatedRows <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区内容状态更新失败，请刷新后重试");
        }
        recordGovernanceAuditLog(operationContext, communityPost, auditTargetType, action, nextReviewStatus, request.adminNotes());
        return toPostResponse(requireAdminPost(postId));
    }

    /**
     * 治理审计记录以内容本身为目标，detail 中保留状态前后值和备注，
     * 便于后台按帖子或问答维度追溯人工下架/恢复决策。
     */
    private void recordGovernanceAuditLog(
        AdminOperationContext operationContext,
        CommunityPostEntity communityPost,
        String auditTargetType,
        String action,
        String nextReviewStatus,
        String adminNotes
    ) {
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            auditTargetType,
            String.valueOf(communityPost.getPostId()),
            auditTargetType + "_" + action,
            Map.of(
                "action", action,
                "post_type", communityPost.getPostType(),
                "review_status_before", communityPost.getReviewStatus(),
                "review_status_after", nextReviewStatus,
                "visibility", communityPost.getVisibility(),
                "author_user_id", String.valueOf(communityPost.getAuthor().getUserId()),
                "admin_notes", adminNotes == null ? "" : adminNotes
            )
        );
    }

    private void requirePostType(
        CommunityPostEntity communityPost,
        String requiredPostType,
        ResponseCode responseCode
    ) {
        if (!requiredPostType.equals(communityPost.getPostType())) {
            throw new BusinessException(responseCode);
        }
    }

    private List<CommunityPostEntity> toPostEntities(List<CommunityPostDataObject> dataObjects) {
        return dataObjects.stream()
            .map(communityPostConverter::toEntity)
            .toList();
    }

    private List<CommunityPostResponse> toPostResponses(List<CommunityPostEntity> posts) {
        return posts.stream()
            .map(this::toPostResponse)
            .toList();
    }

    private CommunityPostResponse toPostResponse(CommunityPostEntity post) {
        List<MediaAssetResponse> mediaAssets = mediaAssetApplicationService.listUploadedMediaAssetResponses(
            post.getMediaAssetIds()
        );
        return communityPostConverter.toResponse(post, mediaAssets);
    }

    private void createModerationTaskForPost(CommunityPostEntity post) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("post_id", String.valueOf(post.getPostId()));
        snapshot.put("post_type", post.getPostType());
        snapshot.put("title", post.getTitle());
        snapshot.put("content", post.getContent());
        snapshot.put("visibility", post.getVisibility());
        snapshot.put("media_asset_ids", post.getMediaAssetIds());
        snapshot.put("source_daily_log_id", post.getSourceDailyLogId() == null ? null : String.valueOf(post.getSourceDailyLogId()));
        snapshot.put("author_user_id", String.valueOf(post.getAuthor().getUserId()));
        /*
         * 当前没有第三方审核供应商，公开内容只能先进入待审核队列；
         * 用户侧公开流继续只读取 approved，确保待审或拒绝内容不会被曝光。
         */
        moderationTaskApplicationService.createCommunityPostReviewTask(
            post.getPostId(),
            POST_TYPE_QA.equals(post.getPostType()),
            resolveModerationContentType(post),
            snapshot
        );
    }

    private String resolveModerationContentType(CommunityPostEntity post) {
        if (POST_TYPE_QA.equals(post.getPostType())) {
            return "qa";
        }
        if (POST_TYPE_VIDEO.equals(post.getPostType())) {
            return "video";
        }
        if (post.getMediaAssetIds() != null && !post.getMediaAssetIds().isEmpty()) {
            return "image_text";
        }
        return "text";
    }

    private PetProfileEntity requirePet(Long petId) {
        PetProfileEntity pet = petEntityConverter.toEntity(petPersistenceMapper.findPetById(petId));
        if (pet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return pet;
    }

    private PetProfileEntity requireAccessiblePet(Long currentUserId, Long petId) {
        PetProfileEntity pet = petEntityConverter.toEntity(
            petPersistenceMapper.findAccessiblePetById(currentUserId, petId)
        );
        if (pet == null) {
            throw new BusinessException(ResponseCode.PET_PERMISSION_DENIED, "无权使用该宠物发布社区帖子");
        }
        return pet;
    }

    private UserProfileEntity requireUser(Long userId) {
        UserProfileEntity user = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(userId));
        if (user == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private CommunityTopicEntity requireActiveTopic(Long topicId) {
        CommunityTopicEntity topic = communityPostConverter.toEntity(
            communityPersistenceMapper.findActiveTopicById(topicId)
        );
        if (topic == null) {
            throw new BusinessException(ResponseCode.COMMUNITY_TOPIC_NOT_FOUND);
        }
        return topic;
    }

    private void requireFollowTarget(Long currentUserId, Long followedUserId) {
        if (followedUserId == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "被关注用户不能为空");
        }
        if (currentUserId.equals(followedUserId)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不能关注自己");
        }
        if (!userPersistenceMapper.existsActiveUserById(followedUserId)) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "被关注用户不存在");
        }
    }

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return TAB_RECOMMENDED;
        }
        String normalizedTab = tab.trim().toLowerCase();
        if (!SUPPORTED_FEED_TABS.contains(normalizedTab)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区信息流类型不支持");
        }
        return normalizedTab;
    }

    private String normalizePostType(String postType) {
        String normalizedPostType = postType == null || postType.isBlank()
            ? POST_TYPE_IMAGE_TEXT
            : postType.trim().toLowerCase();
        if (!SUPPORTED_POST_TYPES.contains(normalizedPostType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "帖子类型不支持");
        }
        return normalizedPostType;
    }

    private String normalizeNullablePostType(String postType) {
        String normalizedPostType = normalizeNullableText(postType, 30);
        if (normalizedPostType == null) {
            return null;
        }
        normalizedPostType = normalizedPostType.toLowerCase();
        if (!SUPPORTED_POST_TYPES.contains(normalizedPostType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "帖子类型不支持");
        }
        return normalizedPostType;
    }

    private String normalizeVisibility(String visibility) {
        String normalizedVisibility = visibility == null || visibility.isBlank()
            ? VISIBILITY_PUBLIC
            : visibility.trim().toLowerCase();
        if (!SUPPORTED_VISIBILITIES.contains(normalizedVisibility)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区帖子可见性不支持");
        }
        return normalizedVisibility;
    }

    private String normalizeNullableVisibility(String visibility) {
        String normalizedVisibility = normalizeNullableText(visibility, 20);
        if (normalizedVisibility == null) {
            return null;
        }
        normalizedVisibility = normalizedVisibility.toLowerCase();
        if (!SUPPORTED_VISIBILITIES.contains(normalizedVisibility)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区帖子可见性不支持");
        }
        return normalizedVisibility;
    }

    private String normalizeNullableReviewStatus(String reviewStatus) {
        String normalizedReviewStatus = normalizeNullableText(reviewStatus, 30);
        if (normalizedReviewStatus == null) {
            return null;
        }
        normalizedReviewStatus = normalizedReviewStatus.toLowerCase();
        if (!SUPPORTED_REVIEW_STATUSES.contains(normalizedReviewStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区内容审核状态不支持");
        }
        return normalizedReviewStatus;
    }

    private String normalizeAdminAction(String action) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase();
        if (!SUPPORTED_ADMIN_ACTIONS.contains(normalizedAction)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "社区治理动作不支持");
        }
        return normalizedAction;
    }

    private String normalizePostContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "帖子正文不能为空");
        }
        if (normalizedContent.length() > 5000) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "帖子正文不能超过 5000 字");
        }
        return normalizedContent;
    }

    /**
     * 问答帖需要明确标题便于问题列表扫描；普通图文帖缺省时从正文生成标题。
     */
    private String normalizePostTitle(String title, String content, String postType) {
        String normalizedTitle = normalizeNullableText(title, 100);
        if (POST_TYPE_QA.equals(postType) && normalizedTitle == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "问答标题不能为空");
        }
        if (normalizedTitle != null) {
            return normalizedTitle;
        }
        return buildPostTitle(content);
    }

    private String resolvePostCityCode(UserProfileEntity author, String requestCityCode) {
        String normalizedRequestCityCode = normalizeNullableText(requestCityCode, 32);
        if (normalizedRequestCityCode != null) {
            return normalizedRequestCityCode;
        }
        return normalizeNullableText(author.getCityCode(), 32);
    }

    private String resolveCityCode(Long currentUserId, String cityCode) {
        String normalizedCityCode = normalizeNullableText(cityCode, 32);
        if (normalizedCityCode != null) {
            return normalizedCityCode;
        }
        return normalizeNullableText(requireUser(currentUserId).getCityCode(), 32);
    }

    private String normalizeNullableText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        if (normalizedText.isEmpty()) {
            return null;
        }
        if (normalizedText.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "输入文本长度超过限制");
        }
        return normalizedText;
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
