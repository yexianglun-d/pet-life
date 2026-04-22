package com.petlife.server.modules.family.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.family.converter.FamilyEntityConverter;
import com.petlife.server.modules.family.domain.entity.FamilyInvitationEntity;
import com.petlife.server.modules.family.domain.entity.FamilyMemberEntity;
import com.petlife.server.modules.family.domain.entity.FamilyProfileEntity;
import com.petlife.server.modules.family.dto.request.CreateFamilyInvitationRequest;
import com.petlife.server.modules.family.dto.request.CreateFamilyRequest;
import com.petlife.server.modules.family.dto.request.UpdateFamilyMemberRoleRequest;
import com.petlife.server.modules.family.dto.response.FamilyDetailResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationResponse;
import com.petlife.server.modules.family.dto.response.FamilyInvitationPreviewResponse;
import com.petlife.server.modules.family.dto.response.FamilyMemberResponse;
import com.petlife.server.modules.family.dto.response.FamilySharedPetResponse;
import com.petlife.server.modules.family.persistence.FamilyPersistenceMapper;
import com.petlife.server.modules.family.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.family.persistence.command.CreateFamilyInvitationCommand;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.service.UserBootstrapApplicationService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭共养应用服务。
 *
 * <p>该服务负责家庭详情、邀请、角色调整和成员移除的主链路。
 * 家庭角色判断、共享宠物校验和成员移除后的用户上下文修复都统一收敛在这里，避免页面和控制器承担业务分支。</p>
 */
@Service
public class FamilyApplicationService {

    private final FamilyPersistenceMapper familyPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final PetEntityConverter petEntityConverter;
    private final UserPersistenceMapper userPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final FamilyEntityConverter familyEntityConverter;
    private final UserBootstrapApplicationService userBootstrapApplicationService;

    public FamilyApplicationService(
        FamilyPersistenceMapper familyPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        PetEntityConverter petEntityConverter,
        UserPersistenceMapper userPersistenceMapper,
        UserEntityConverter userEntityConverter,
        FamilyEntityConverter familyEntityConverter,
        UserBootstrapApplicationService userBootstrapApplicationService
    ) {
        this.familyPersistenceMapper = familyPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.petEntityConverter = petEntityConverter;
        this.userPersistenceMapper = userPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.familyEntityConverter = familyEntityConverter;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
    }

    public FamilyDetailResponse getCurrentFamily() {
        Long currentUserId = CurrentUserContext.requireUserId();
        return assembleFamilyDetail(requireCurrentFamilyProfile(currentUserId));
    }

    public FamilyInvitationPreviewResponse getInvitationPreview(String inviteCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfileEntity currentUser = requireCurrentUserProfile(currentUserId);
        FamilyInvitationEntity invitation = requireVisibleInvitation(inviteCode, currentUser);
        FamilyProfileEntity familyProfile = requireFamilyProfileById(invitation.getFamilyId());
        UserProfileEntity inviterUser =
            userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(invitation.getInviterUserId()));

        return familyEntityConverter.toInvitationPreviewResponse(
            invitation,
            familyProfile,
            inviterUser == null ? "宠物家长" : inviterUser.getNickname(),
            listSharedPetResponses(invitation.getFamilyId(), invitation.getSharedPetIds())
        );
    }

    @Transactional
    public FamilyDetailResponse initializeFamily(CreateFamilyRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilyProfileEntity existingFamily = familyEntityConverter.toEntity(
            familyPersistenceMapper.findAccessibleFamilyProfileByUserId(currentUserId)
        );
        if (existingFamily != null) {
            return assembleFamilyDetail(existingFamily);
        }

        UserProfileEntity currentUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(currentUserId));
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        CreateFamilyCommand command = new CreateFamilyCommand();
        command.setOwnerUserId(currentUserId);
        command.setFamilyName(
            request.familyName() == null || request.familyName().isBlank()
                ? currentUser.getNickname() + "的家庭"
                : request.familyName().trim()
        );
        familyPersistenceMapper.insertFamily(command);
        familyPersistenceMapper.insertFamilyMember(command.getId(), currentUserId, "owner");
        userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(currentUserId);
        return getCurrentFamily();
    }

    @Transactional
    public FamilyInvitationResponse createInvitation(CreateFamilyInvitationRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilyProfileEntity familyProfile = requireCurrentFamilyProfile(currentUserId);
        requireManagementRole(familyProfile.getCurrentUserRole());

        String invitationRole = normalizeInvitationRole(request.role());
        List<Long> accessiblePetIds = listAccessibleFamilyPets(
            familyProfile.getFamilyId(),
            currentUserId,
            familyProfile.getCurrentUserRole()
        ).stream()
            .map(FamilySharedPetResponse::petId)
            .map(Long::valueOf)
            .toList();
        List<Long> sharedPetIds = normalizeSharedPetIds(request.sharedPetIds(), accessiblePetIds);

        UserProfileEntity inviteeUser =
            userEntityConverter.toEntity(userPersistenceMapper.findUserProfileByMobile(request.inviteeMobile().trim()));
        if (inviteeUser != null) {
            if (inviteeUser.getUserId().equals(currentUserId)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "不能邀请自己加入家庭");
            }
            if (familyPersistenceMapper.findJoinedMemberByFamilyAndUserId(
                familyProfile.getFamilyId(),
                inviteeUser.getUserId()
            ) != null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "该用户已经是当前家庭成员");
            }
        }

        CreateFamilyInvitationCommand command = new CreateFamilyInvitationCommand();
        command.setFamilyId(familyProfile.getFamilyId());
        command.setInviterUserId(currentUserId);
        command.setInviteeMobile(request.inviteeMobile().trim());
        command.setInviteeUserId(inviteeUser == null ? null : inviteeUser.getUserId());
        command.setRole(invitationRole);
        command.setSharedPetIdsJson(familyEntityConverter.toSharedPetIdsJson(sharedPetIds));
        command.setInviteCode(UUID.randomUUID().toString().replace("-", ""));
        command.setExpiredAt(LocalDateTime.now().plusDays(7));
        familyPersistenceMapper.insertFamilyInvitation(command);
        return familyEntityConverter.toInvitationResponse(
            familyEntityConverter.toEntity(familyPersistenceMapper.findInvitationById(command.getId()))
        );
    }

    /**
     * 接受邀请后必须立刻把当前宠物切到当前邀请允许访问的共享宠物。
     *
     * <p>当前系统的“活跃家庭上下文”由当前宠物反推。若只插入成员关系而不切换当前宠物，
     * 用户仍会停留在自己的默认家庭里，家庭页和后续宠物操作都会出现上下文错乱。</p>
     */
    @Transactional
    public FamilyDetailResponse acceptInvitation(String inviteCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfileEntity currentUser = requireCurrentUserProfile(currentUserId);
        FamilyInvitationEntity invitation = requireActionableInvitation(inviteCode, currentUser);
        if (familyPersistenceMapper.findJoinedMemberByFamilyAndUserId(invitation.getFamilyId(), currentUserId) != null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前用户已经加入该家庭");
        }

        Long targetPetId = resolveInvitationCurrentPetId(invitation);
        if (targetPetId == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前邀请已无可共享宠物，请让邀请人重新发起邀请");
        }

        int acceptedRows = familyPersistenceMapper.acceptInvitation(invitation.getInvitationId(), currentUserId);
        if (acceptedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "邀请码状态已变化，请刷新后重试");
        }

        familyPersistenceMapper.insertFamilyMember(invitation.getFamilyId(), currentUserId, invitation.getRole());
        userPersistenceMapper.updateCurrentPet(currentUserId, targetPetId);
        return getCurrentFamily();
    }

    @Transactional
    public FamilyInvitationPreviewResponse rejectInvitation(String inviteCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UserProfileEntity currentUser = requireCurrentUserProfile(currentUserId);
        FamilyInvitationEntity invitation = requireActionableInvitation(inviteCode, currentUser);

        int rejectedRows = familyPersistenceMapper.rejectInvitation(invitation.getInvitationId(), currentUserId);
        if (rejectedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "邀请码状态已变化，请刷新后重试");
        }
        return getInvitationPreview(inviteCode);
    }

    @Transactional
    public FamilyMemberResponse updateMemberRole(Long memberId, UpdateFamilyMemberRoleRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilyProfileEntity familyProfile = requireCurrentFamilyProfile(currentUserId);
        FamilyMemberEntity targetMember = requireJoinedMember(memberId);
        requireSameFamily(familyProfile.getFamilyId(), targetMember.getFamilyId());

        String targetRole = normalizeInvitationRole(request.role());
        requireRoleChangePermission(familyProfile.getCurrentUserRole(), currentUserId, targetMember, targetRole);
        familyPersistenceMapper.updateFamilyMemberRole(memberId, targetRole);
        return familyEntityConverter.toMemberResponse(
            familyEntityConverter.toEntity(familyPersistenceMapper.findJoinedMemberById(memberId))
        );
    }

    @Transactional
    public void removeMember(Long memberId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilyProfileEntity familyProfile = requireCurrentFamilyProfile(currentUserId);
        FamilyMemberEntity targetMember = requireJoinedMember(memberId);
        requireSameFamily(familyProfile.getFamilyId(), targetMember.getFamilyId());
        requireRemovePermission(familyProfile.getCurrentUserRole(), currentUserId, targetMember);

        familyPersistenceMapper.deleteFamilyMember(memberId);
        userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(targetMember.getUserId());
    }

    private FamilyDetailResponse assembleFamilyDetail(FamilyProfileEntity familyProfile) {
        Long currentUserId = CurrentUserContext.requireUserId();
        List<FamilyMemberResponse> members = familyPersistenceMapper.listJoinedMembersByFamilyId(familyProfile.getFamilyId()).stream()
            .map(familyEntityConverter::toEntity)
            .map(familyEntityConverter::toMemberResponse)
            .toList();
        List<FamilySharedPetResponse> sharedPets = listAccessibleFamilyPets(
            familyProfile.getFamilyId(),
            currentUserId,
            familyProfile.getCurrentUserRole()
        );
        List<FamilyInvitationResponse> pendingInvitations = canViewPendingInvitations(familyProfile.getCurrentUserRole())
            ? familyPersistenceMapper.listPendingInvitationsByFamilyId(familyProfile.getFamilyId()).stream()
                .map(familyEntityConverter::toEntity)
                .map(familyEntityConverter::toInvitationResponse)
                .toList()
            : List.of();

        return familyEntityConverter.toDetailResponse(familyProfile, members, sharedPets, pendingInvitations);
    }

    private List<PetDetailResponse> listAllFamilyPets(Long familyId) {
        return petPersistenceMapper.listPetsByFamilyId(familyId).stream()
            .map(petEntityConverter::toEntity)
            .map(petEntityConverter::toPetDetailResponse)
            .toList();
    }

    private List<FamilySharedPetResponse> listAccessibleFamilyPets(Long familyId, Long userId, String currentUserRole) {
        if ("owner".equals(currentUserRole)) {
            return listAllFamilyPets(familyId).stream()
                .map(familyEntityConverter::toSharedPetResponse)
                .toList();
        }

        FamilyInvitationEntity acceptedInvitation = familyEntityConverter.toEntity(
            familyPersistenceMapper.findLatestAcceptedInvitationByFamilyAndUserId(familyId, userId)
        );
        if (acceptedInvitation == null || acceptedInvitation.getSharedPetIds().isEmpty()) {
            return listAllFamilyPets(familyId).stream()
                .map(familyEntityConverter::toSharedPetResponse)
                .toList();
        }

        return listSharedPetResponses(familyId, acceptedInvitation.getSharedPetIds());
    }

    private List<FamilySharedPetResponse> listSharedPetResponses(Long familyId, List<Long> sharedPetIds) {
        Map<Long, FamilySharedPetResponse> familyPetMap = new LinkedHashMap<>();
        listAllFamilyPets(familyId).stream()
            .map(familyEntityConverter::toSharedPetResponse)
            .forEach(pet -> familyPetMap.put(Long.valueOf(pet.petId()), pet));

        return sharedPetIds.stream()
            .distinct()
            .map(familyPetMap::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private FamilyProfileEntity requireCurrentFamilyProfile(Long userId) {
        FamilyProfileEntity familyProfile =
            familyEntityConverter.toEntity(familyPersistenceMapper.findAccessibleFamilyProfileByUserId(userId));
        if (familyProfile == null) {
            throw new BusinessException(ResponseCode.FAMILY_NOT_FOUND);
        }
        return familyProfile;
    }

    private FamilyProfileEntity requireFamilyProfileById(Long familyId) {
        FamilyProfileEntity familyProfile =
            familyEntityConverter.toEntity(familyPersistenceMapper.findFamilyProfileById(familyId));
        if (familyProfile == null) {
            throw new BusinessException(ResponseCode.FAMILY_NOT_FOUND);
        }
        return familyProfile;
    }

    private UserProfileEntity requireCurrentUserProfile(Long userId) {
        UserProfileEntity currentUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(userId));
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return currentUser;
    }

    private FamilyInvitationEntity requireVisibleInvitation(String inviteCode, UserProfileEntity currentUser) {
        FamilyInvitationEntity invitation = requireInvitationByCode(inviteCode);
        validateInvitationInvitee(currentUser, invitation);
        return invitation;
    }

    private FamilyInvitationEntity requireActionableInvitation(String inviteCode, UserProfileEntity currentUser) {
        FamilyInvitationEntity invitation = requireVisibleInvitation(inviteCode, currentUser);
        if (!"pending".equals(invitation.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前邀请码已处理或已失效");
        }
        return invitation;
    }

    private FamilyInvitationEntity requireInvitationByCode(String inviteCode) {
        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        FamilyInvitationEntity invitation =
            familyEntityConverter.toEntity(familyPersistenceMapper.findInvitationByCode(normalizedInviteCode));
        if (invitation == null) {
            throw new BusinessException(ResponseCode.FAMILY_INVITATION_NOT_FOUND);
        }
        return refreshExpiredInvitationIfNecessary(invitation);
    }

    private FamilyInvitationEntity refreshExpiredInvitationIfNecessary(FamilyInvitationEntity invitation) {
        if (!"pending".equals(invitation.getStatus())) {
            return invitation;
        }
        if (invitation.getExpiredAt() == null || invitation.getExpiredAt().isAfter(LocalDateTime.now())) {
            return invitation;
        }

        familyPersistenceMapper.expireInvitation(invitation.getInvitationId());
        FamilyInvitationEntity refreshedInvitation =
            familyEntityConverter.toEntity(familyPersistenceMapper.findInvitationById(invitation.getInvitationId()));
        return refreshedInvitation == null ? invitation : refreshedInvitation;
    }

    private void validateInvitationInvitee(UserProfileEntity currentUser, FamilyInvitationEntity invitation) {
        if (invitation.getInviteeUserId() != null && !invitation.getInviteeUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前用户无权处理该邀请");
        }
        if (!currentUser.getMobile().equals(invitation.getInviteeMobile())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前用户无权处理该邀请");
        }
    }

    private Long resolveInvitationCurrentPetId(FamilyInvitationEntity invitation) {
        return listSharedPetResponses(invitation.getFamilyId(), invitation.getSharedPetIds()).stream()
            .findFirst()
            .map(FamilySharedPetResponse::petId)
            .map(Long::valueOf)
            .orElse(null);
    }

    private boolean canViewPendingInvitations(String currentUserRole) {
        return "owner".equals(currentUserRole) || "admin".equals(currentUserRole);
    }

    private FamilyMemberEntity requireJoinedMember(Long memberId) {
        FamilyMemberEntity familyMember =
            familyEntityConverter.toEntity(familyPersistenceMapper.findJoinedMemberById(memberId));
        if (familyMember == null) {
            throw new BusinessException(ResponseCode.FAMILY_MEMBER_NOT_FOUND);
        }
        return familyMember;
    }

    private void requireManagementRole(String currentUserRole) {
        if (!"owner".equals(currentUserRole) && !"admin".equals(currentUserRole)) {
            throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "当前角色无权管理家庭成员");
        }
    }

    /**
     * 成员角色管理必须遵守最小授权原则：
     * owner 不能降级或移除 owner；admin 只能管理普通成员，避免出现横向越权。
     */
    private void requireRoleChangePermission(
        String currentUserRole,
        Long currentUserId,
        FamilyMemberEntity targetMember,
        String targetRole
    ) {
        if (currentUserId.equals(targetMember.getUserId())) {
            throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "暂不支持修改自己的家庭角色");
        }
        if ("owner".equals(targetMember.getRole())) {
            throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "拥有者角色不可修改");
        }

        if ("owner".equals(currentUserRole)) {
            return;
        }
        if ("admin".equals(currentUserRole) && "member".equals(targetMember.getRole()) && !"owner".equals(targetRole)) {
            return;
        }

        throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "当前角色无权修改该成员权限");
    }

    private void requireRemovePermission(
        String currentUserRole,
        Long currentUserId,
        FamilyMemberEntity targetMember
    ) {
        if (currentUserId.equals(targetMember.getUserId())) {
            throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "暂不支持移除自己");
        }
        if ("owner".equals(targetMember.getRole())) {
            throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "拥有者不可被移除");
        }

        if ("owner".equals(currentUserRole)) {
            return;
        }
        if ("admin".equals(currentUserRole) && "member".equals(targetMember.getRole())) {
            return;
        }

        throw new BusinessException(ResponseCode.FAMILY_ROLE_FORBIDDEN, "当前角色无权移除该成员");
    }

    private void requireSameFamily(Long expectedFamilyId, Long actualFamilyId) {
        if (!expectedFamilyId.equals(actualFamilyId)) {
            throw new BusinessException(ResponseCode.FAMILY_MEMBER_NOT_FOUND);
        }
    }

    private String normalizeInvitationRole(String role) {
        String normalizedRole = role == null ? "" : role.trim();
        if (!"admin".equals(normalizedRole) && !"member".equals(normalizedRole)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "家庭角色仅支持 admin 或 member");
        }
        return normalizedRole;
    }

    private String normalizeInviteCode(String inviteCode) {
        String normalizedInviteCode = inviteCode == null ? "" : inviteCode.trim();
        if (normalizedInviteCode.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "邀请码不能为空");
        }
        return normalizedInviteCode;
    }

    private List<Long> normalizeSharedPetIds(List<String> sharedPetIds, List<Long> accessiblePetIds) {
        Set<Long> accessiblePetIdSet = Set.copyOf(accessiblePetIds);
        List<Long> normalizedPetIds = sharedPetIds.stream()
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(item -> {
                try {
                    return Long.valueOf(item);
                } catch (NumberFormatException ex) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "共享宠物 ID 格式不合法");
                }
            })
            .filter(accessiblePetIdSet::contains)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
        if (normalizedPetIds.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "至少选择一只当前角色可共享的宠物");
        }
        return normalizedPetIds;
    }
}
