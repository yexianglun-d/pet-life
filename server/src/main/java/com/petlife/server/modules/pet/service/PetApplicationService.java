package com.petlife.server.modules.pet.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.persistence.record.PetProfilePersistenceRecord;
import com.petlife.server.modules.pet.dto.request.CreatePetRequest;
import com.petlife.server.modules.pet.dto.request.UpdatePetRequest;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.dto.response.PetSummaryResponse;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.persistence.record.FamilySummaryPersistenceRecord;
import com.petlife.server.modules.user.persistence.record.UserProfilePersistenceRecord;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 宠物应用服务。
 *
 * <p>当前阶段的聚合内容先由服务层直接组装固定示例数据，
 * 目的是为移动端与后台联调提供稳定输出。后续接入健康、日常和提醒模块后，
 * 再替换为真实聚合查询。</p>
 */
@Service
public class PetApplicationService {

    private final PetPersistenceMapper petPersistenceMapper;
    private final UserPersistenceMapper userPersistenceMapper;
    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;

    public PetApplicationService(
        PetPersistenceMapper petPersistenceMapper,
        UserPersistenceMapper userPersistenceMapper,
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        ReminderPersistenceMapper reminderPersistenceMapper,
        DailyLogPersistenceMapper dailyLogPersistenceMapper
    ) {
        this.petPersistenceMapper = petPersistenceMapper;
        this.userPersistenceMapper = userPersistenceMapper;
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
    }

    public List<PetDetailResponse> listPets() {
        Long currentUserId = CurrentUserContext.requireUserId();
        return petPersistenceMapper.listPetsByUserId(currentUserId).stream()
            .map(this::toPetDetailResponse)
            .toList();
    }

    @Transactional
    public PetDetailResponse createPet(CreatePetRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        FamilySummaryPersistenceRecord familySummary =
            userPersistenceMapper.findPrimaryFamilySummaryByUserId(currentUserId);
        if (familySummary == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "当前用户尚未加入家庭");
        }

        CreatePetCommand command = new CreatePetCommand();
        command.setFamilyId(familySummary.familyId());
        command.setOwnerUserId(currentUserId);
        command.setPetName(request.petName());
        command.setPetType(request.petType());
        command.setBreed(request.breed());
        command.setGender(request.gender());
        command.setBirthday(request.birthday());
        command.setAdoptDate(request.adoptDate());
        command.setNeuterStatus(toNeuterStatusValue(request.neuterStatus()));
        command.setAvatarUrl(request.avatarAssetId());
        petPersistenceMapper.insertPet(command);

        UserProfilePersistenceRecord currentUser = userPersistenceMapper.findUserProfileById(currentUserId);
        if (currentUser.currentPetId() == null) {
            userPersistenceMapper.updateCurrentPet(currentUserId, command.getId());
        }
        return toPetDetailResponse(petPersistenceMapper.findPetById(command.getId()));
    }

    public PetDetailResponse getPet(Long petId) {
        return toPetDetailResponse(requireAccessiblePet(petId));
    }

    @Transactional
    public PetDetailResponse updatePet(Long petId, UpdatePetRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        int updatedRows = petPersistenceMapper.updatePetSnapshot(
            petId,
            currentUserId,
            request.petName(),
            request.petType(),
            request.breed(),
            request.gender(),
            request.birthday(),
            request.adoptDate(),
            toNeuterStatusValue(request.neuterStatus()),
            request.avatarAssetId()
        );
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return toPetDetailResponse(requireAccessiblePet(petId));
    }

    public PetSummaryResponse getPetSummary(Long petId) {
        PetDetailResponse petDetail = getPet(petId);

        return new PetSummaryResponse(
            petDetail,
            Math.toIntExact(reminderPersistenceMapper.listRemindersByPetId(petId).stream()
                .filter(reminder -> "pending".equals(reminder.status()))
                .count()),
            healthRecordPersistenceMapper.listHealthRecordsByPetId(petId).stream()
                .limit(3)
                .map(record -> record.title())
                .toList(),
            dailyLogPersistenceMapper.listDailyLogsByPetId(petId).stream()
                .limit(3)
                .map(record -> record.content())
                .toList()
        );
    }

    private PetProfilePersistenceRecord requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        PetProfilePersistenceRecord petProfile = petPersistenceMapper.findAccessiblePetById(currentUserId, petId);
        if (petProfile == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petProfile;
    }

    private PetDetailResponse toPetDetailResponse(PetProfilePersistenceRecord petProfile) {
        return new PetDetailResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed(),
            petProfile.gender(),
            petProfile.birthday(),
            petProfile.adoptDate(),
            toNeuterStatusLabel(petProfile.neuterStatus()),
            petProfile.avatarUrl(),
            DateTimeConverters.toOffsetDateTime(petProfile.createdAt()),
            DateTimeConverters.toOffsetDateTime(petProfile.updatedAt())
        );
    }

    private Integer toNeuterStatusValue(String neuterStatus) {
        if (neuterStatus == null || neuterStatus.isBlank() || "unknown".equals(neuterStatus)) {
            return null;
        }
        return switch (neuterStatus) {
            case "completed", "yes", "true", "1" -> 1;
            default -> 0;
        };
    }

    private String toNeuterStatusLabel(Integer neuterStatus) {
        if (neuterStatus == null) {
            return "unknown";
        }
        return neuterStatus == 1 ? "completed" : "pending";
    }
}
