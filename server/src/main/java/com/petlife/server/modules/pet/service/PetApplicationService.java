package com.petlife.server.modules.pet.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.converter.DailyLogEntityConverter;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.family.converter.FamilyEntityConverter;
import com.petlife.server.modules.family.domain.entity.FamilyMemberEntity;
import com.petlife.server.modules.family.persistence.FamilyPersistenceMapper;
import com.petlife.server.modules.family.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.pet.converter.AdminPetConverter;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.AdminPetEntity;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.dto.request.ArchivePetRequest;
import com.petlife.server.modules.pet.dto.request.AdminRepairPetRequest;
import com.petlife.server.modules.pet.dto.request.CreatePetRequest;
import com.petlife.server.modules.pet.dto.request.UpdatePetRequest;
import com.petlife.server.modules.pet.dto.response.AdminPetResponse;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.dto.response.PetSummaryResponse;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.command.ArchivePetCommand;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.persistence.command.DeletePetCommand;
import com.petlife.server.modules.pet.persistence.command.UpdatePetProfileCommand;
import com.petlife.server.modules.reminder.converter.ReminderEntityConverter;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.service.UserBootstrapApplicationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 宠物应用服务。
 *
 * <p>该服务负责宠物主档的创建、编辑、详情与摘要聚合，
 * 并在应用层统一收敛宠物权限校验和领域对象转换。</p>
 */
@Service
public class PetApplicationService {

    private final PetPersistenceMapper petPersistenceMapper;
    private final UserPersistenceMapper userPersistenceMapper;
    private final FamilyPersistenceMapper familyPersistenceMapper;
    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final FamilyEntityConverter familyEntityConverter;
    private final AdminPetConverter adminPetConverter;
    private final PetEntityConverter petEntityConverter;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final ReminderEntityConverter reminderEntityConverter;
    private final DailyLogEntityConverter dailyLogEntityConverter;
    private final UserBootstrapApplicationService userBootstrapApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public PetApplicationService(
        PetPersistenceMapper petPersistenceMapper,
        UserPersistenceMapper userPersistenceMapper,
        FamilyPersistenceMapper familyPersistenceMapper,
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        ReminderPersistenceMapper reminderPersistenceMapper,
        DailyLogPersistenceMapper dailyLogPersistenceMapper,
        UserEntityConverter userEntityConverter,
        FamilyEntityConverter familyEntityConverter,
        AdminPetConverter adminPetConverter,
        PetEntityConverter petEntityConverter,
        HealthRecordEntityConverter healthRecordEntityConverter,
        ReminderEntityConverter reminderEntityConverter,
        DailyLogEntityConverter dailyLogEntityConverter,
        UserBootstrapApplicationService userBootstrapApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.petPersistenceMapper = petPersistenceMapper;
        this.userPersistenceMapper = userPersistenceMapper;
        this.familyPersistenceMapper = familyPersistenceMapper;
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.familyEntityConverter = familyEntityConverter;
        this.adminPetConverter = adminPetConverter;
        this.petEntityConverter = petEntityConverter;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.reminderEntityConverter = reminderEntityConverter;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public List<PetDetailResponse> listPets() {
        Long currentUserId = CurrentUserContext.requireUserId();
        return petPersistenceMapper.listPetsByUserId(currentUserId).stream()
            .map(petEntityConverter::toEntity)
            .map(petEntityConverter::toPetDetailResponse)
            .toList();
    }

    public List<AdminPetResponse> listAdminPets(
        String keyword,
        String petName,
        String petType,
        String status,
        String ownerMobile,
        Long familyId
    ) {
        String normalizedKeyword = normalizeOptionalText(keyword, 100, "搜索关键词长度不能超过 100 个字符");
        String normalizedPetName = normalizeOptionalText(petName, 50, "宠物名称长度不能超过 50 个字符");
        String normalizedPetType = normalizeOptionalPetType(petType);
        String normalizedStatus = normalizeOptionalPetStatus(status);
        String normalizedOwnerMobile = normalizeOptionalText(ownerMobile, 20, "主人手机号长度不能超过 20 个字符");

        // 后台宠物查询按存量主档读取，包含已归档但未软删的宠物，便于运营核查归属关系。
        return petPersistenceMapper
            .listAdminPets(
                normalizedKeyword,
                normalizedPetName,
                normalizedPetType,
                normalizedStatus,
                normalizedOwnerMobile,
                familyId
            )
            .stream()
            .map(adminPetConverter::toEntity)
            .map(adminPetConverter::toResponse)
            .toList();
    }

    public AdminPetResponse getAdminPet(Long petId) {
        AdminPetEntity adminPet = adminPetConverter.toEntity(petPersistenceMapper.findAdminPetById(petId));
        if (adminPet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return adminPetConverter.toResponse(adminPet);
    }

    /**
     * 后台宠物修复工具只处理明确可验证的问题类型。
     *
     * <p>修复前先按当前数据库事实校验主人、家庭与成员关系，避免后台按钮变成任意改数据入口。</p>
     */
    @Transactional
    public AdminPetResponse repairAdminPet(
        Long petId,
        AdminOperationContext operationContext,
        AdminRepairPetRequest request
    ) {
        AdminPetEntity adminPet = requireAdminPet(petId);
        String repairType = normalizePetRepairType(request.repairType());
        switch (repairType) {
            case "family_missing" -> repairMissingFamily(adminPet);
            case "owner_member_missing" -> repairOwnerMember(adminPet);
            case "current_pet_context" -> userBootstrapApplicationService.rebuildCurrentPetContextForPet(petId);
            default -> throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物修复类型不支持");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("repair_type", repairType);
        detail.put("reason", normalizeNullableText(request.reason()));
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            "pet",
            String.valueOf(petId),
            "pet_" + repairType + "_repair",
            detail
        );
        return getAdminPet(petId);
    }

    @Transactional
    public PetDetailResponse createPet(CreatePetRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilySummaryEntity familySummary =
            userEntityConverter.toEntity(userPersistenceMapper.findPrimaryFamilySummaryByUserId(currentUserId));
        if (familySummary == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "当前用户尚未加入家庭");
        }

        CreatePetCommand command = new CreatePetCommand();
        command.setFamilyId(familySummary.getFamilyId());
        command.setOwnerUserId(currentUserId);
        command.setPetName(request.petName());
        command.setPetType(request.petType());
        command.setBreed(request.breed());
        command.setGender(request.gender());
        command.setBirthday(request.birthday());
        command.setAdoptDate(request.adoptDate());
        command.setNeuterStatus(petEntityConverter.toNeuterStatusValue(request.neuterStatus()));
        command.setAvatarUrl(request.avatarAssetId());
        command.setWeightKg(petEntityConverter.toWeightKg(request.weightKg()));
        command.setAllergyNotes(normalizeNullableText(request.allergyNotes()));
        command.setMedicalHistory(normalizeNullableText(request.medicalHistory()));
        petPersistenceMapper.insertPet(command);

        UserProfileEntity currentUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(currentUserId));
        if (currentUser.getCurrentPetId() == null) {
            userPersistenceMapper.updateCurrentPet(currentUserId, command.getId());
        }
        return petEntityConverter.toPetDetailResponse(petEntityConverter.toEntity(petPersistenceMapper.findPetById(command.getId())));
    }

    public PetDetailResponse getPet(Long petId) {
        return petEntityConverter.toPetDetailResponse(requireAccessiblePet(petId));
    }

    @Transactional
    public PetDetailResponse updatePet(Long petId, UpdatePetRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UpdatePetProfileCommand command = new UpdatePetProfileCommand();
        command.setPetId(petId);
        command.setUserId(currentUserId);
        command.setPetName(normalizeNullableText(request.petName()));
        command.setPetType(normalizeNullableText(request.petType()));
        command.setBreed(normalizeNullableText(request.breed()));
        command.setGender(normalizeNullableText(request.gender()));
        command.setBirthday(request.birthday());
        command.setAdoptDate(request.adoptDate());
        command.setNeuterStatus(petEntityConverter.toNeuterStatusValue(request.neuterStatus()));
        command.setAvatarUrl(normalizeNullableText(request.avatarAssetId()));
        command.setWeightKg(petEntityConverter.toWeightKg(request.weightKg()));
        command.setAllergyNotes(normalizeNullableText(request.allergyNotes()));
        command.setMedicalHistory(normalizeNullableText(request.medicalHistory()));

        int updatedRows = petPersistenceMapper.updatePetSnapshot(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petEntityConverter.toPetDetailResponse(requireAccessiblePet(petId));
    }

    @Transactional
    public void archivePet(Long petId, ArchivePetRequest request) {
        PetProfileEntity petProfile = requireAccessiblePet(petId);
        requirePetLifecyclePermission(petProfile);

        ArchivePetCommand command = new ArchivePetCommand();
        command.setPetId(petId);
        command.setArchiveStatus(normalizeArchiveStatus(request.archiveStatus()));
        if (petPersistenceMapper.archivePet(command) == 0) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        userBootstrapApplicationService.rebuildCurrentPetContextForPet(petId);
    }

    @Transactional
    public void deletePet(Long petId) {
        PetProfileEntity petProfile = requireAccessiblePet(petId);
        requirePetLifecyclePermission(petProfile);

        DeletePetCommand command = new DeletePetCommand();
        command.setPetId(petId);
        if (petPersistenceMapper.softDeletePet(command) == 0) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        userBootstrapApplicationService.rebuildCurrentPetContextForPet(petId);
    }

    public PetSummaryResponse getPetSummary(Long petId) {
        PetDetailResponse petDetail = getPet(petId);

        return new PetSummaryResponse(
            petDetail,
            Math.toIntExact(reminderPersistenceMapper.listRemindersByPetId(petId).stream()
                .map(reminderEntityConverter::toEntity)
                .filter(reminder -> "pending".equals(reminder.getStatus()))
                .count()),
            healthRecordPersistenceMapper.listHealthRecordsByPetId(petId).stream()
                .limit(3)
                .map(healthRecordEntityConverter::toEntity)
                .map(HealthRecordEntity::getTitle)
                .toList(),
            dailyLogPersistenceMapper.listDailyLogsByPetId(petId).stream()
                .limit(3)
                .map(dailyLogEntityConverter::toEntity)
                .map(DailyLogEntity::getContent)
                .toList()
        );
    }

    private AdminPetEntity requireAdminPet(Long petId) {
        AdminPetEntity adminPet = adminPetConverter.toEntity(petPersistenceMapper.findAdminPetById(petId));
        if (adminPet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return adminPet;
    }

    private void repairMissingFamily(AdminPetEntity adminPet) {
        PetProfileEntity petProfile = adminPet.getPetProfile();
        if (petProfile.getFamilyId() != null && adminPet.getFamilyStatus() != null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前宠物已有家庭归属，无需执行家庭缺失修复");
        }
        UserProfileEntity ownerProfile =
            userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(petProfile.getOwnerUserId()));
        if (ownerProfile == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物主人不存在或已被禁用，无法创建修复家庭");
        }

        CreateFamilyCommand command = new CreateFamilyCommand();
        command.setOwnerUserId(ownerProfile.getUserId());
        command.setFamilyName(ownerProfile.getNickname() + "的家庭");
        familyPersistenceMapper.insertFamily(command);
        familyPersistenceMapper.insertFamilyMember(command.getId(), ownerProfile.getUserId(), "owner");
        petPersistenceMapper.updatePetFamily(petProfile.getPetId(), command.getId());
        userBootstrapApplicationService.rebuildCurrentPetContextForPet(petProfile.getPetId());
    }

    private void repairOwnerMember(AdminPetEntity adminPet) {
        PetProfileEntity petProfile = adminPet.getPetProfile();
        if (petProfile.getFamilyId() == null || adminPet.getFamilyStatus() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前宠物没有有效家庭，请先执行家庭缺失修复");
        }
        if (!userPersistenceMapper.existsActiveUserById(petProfile.getOwnerUserId())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物主人不存在或已被禁用");
        }

        var family = familyPersistenceMapper.findAdminFamilyById(petProfile.getFamilyId());
        String ownerRole = family != null && petProfile.getOwnerUserId().equals(family.ownerUserId()) ? "owner" : "admin";
        familyPersistenceMapper.insertFamilyMember(petProfile.getFamilyId(), petProfile.getOwnerUserId(), ownerRole);
        userBootstrapApplicationService.rebuildCurrentPetContextForPet(petProfile.getPetId());
    }

    private PetProfileEntity requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        PetProfileEntity petProfile =
            petEntityConverter.toEntity(petPersistenceMapper.findAccessiblePetById(currentUserId, petId));
        if (petProfile == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petProfile;
    }

    /**
     * 删除和归档会直接改变家庭成员的当前宠物上下文，因此只允许家庭 owner/admin 操作。
     */
    private void requirePetLifecyclePermission(PetProfileEntity petProfile) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilyMemberEntity familyMember = familyEntityConverter.toEntity(
            familyPersistenceMapper.findJoinedMemberByFamilyAndUserId(petProfile.getFamilyId(), currentUserId)
        );
        if (familyMember == null) {
            throw new BusinessException(ResponseCode.PET_PERMISSION_DENIED, "当前用户无权管理该宠物");
        }
        if ("owner".equals(familyMember.getRole()) || "admin".equals(familyMember.getRole())) {
            return;
        }
        throw new BusinessException(ResponseCode.PET_PERMISSION_DENIED, "当前角色无权删除或归档宠物");
    }

    private String normalizeArchiveStatus(String archiveStatus) {
        String normalizedArchiveStatus = archiveStatus == null ? "" : archiveStatus.trim();
        if ("memorial".equals(normalizedArchiveStatus) || "rehomed".equals(normalizedArchiveStatus)) {
            return normalizedArchiveStatus;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物归档状态仅支持 memorial 或 rehomed");
    }

    private String normalizeOptionalPetType(String petType) {
        String normalizedPetType = normalizeOptionalText(petType, 20, "宠物类型长度不能超过 20 个字符");
        if (normalizedPetType == null) {
            return null;
        }
        if (!"cat".equals(normalizedPetType) && !"dog".equals(normalizedPetType) && !"other".equals(normalizedPetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物类型仅支持 cat、dog 或 other");
        }
        return normalizedPetType;
    }

    private String normalizeOptionalPetStatus(String status) {
        String normalizedStatus = normalizeOptionalText(status, 20, "宠物状态长度不能超过 20 个字符");
        if (normalizedStatus == null) {
            return null;
        }
        if (!"active".equals(normalizedStatus) && !"memorial".equals(normalizedStatus) && !"rehomed".equals(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物状态仅支持 active、memorial 或 rehomed");
        }
        return normalizedStatus;
    }

    private String normalizeOptionalText(String value, int maxLength, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, errorMessage);
        }
        return normalizedValue;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private String normalizePetRepairType(String repairType) {
        String normalizedRepairType = normalizeOptionalText(repairType, 50, "宠物修复类型长度不能超过 50 个字符");
        if (normalizedRepairType == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物修复类型不能为空");
        }
        if (!"family_missing".equals(normalizedRepairType)
            && !"owner_member_missing".equals(normalizedRepairType)
            && !"current_pet_context".equals(normalizedRepairType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "宠物修复类型不支持");
        }
        return normalizedRepairType;
    }
}
