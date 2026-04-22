package com.petlife.server.modules.pet.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.converter.DailyLogEntityConverter;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.dto.request.CreatePetRequest;
import com.petlife.server.modules.pet.dto.request.UpdatePetRequest;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.dto.response.PetSummaryResponse;
import com.petlife.server.modules.reminder.converter.ReminderEntityConverter;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import java.util.List;
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
    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final PetEntityConverter petEntityConverter;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final ReminderEntityConverter reminderEntityConverter;
    private final DailyLogEntityConverter dailyLogEntityConverter;

    public PetApplicationService(
        PetPersistenceMapper petPersistenceMapper,
        UserPersistenceMapper userPersistenceMapper,
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        ReminderPersistenceMapper reminderPersistenceMapper,
        DailyLogPersistenceMapper dailyLogPersistenceMapper,
        UserEntityConverter userEntityConverter,
        PetEntityConverter petEntityConverter,
        HealthRecordEntityConverter healthRecordEntityConverter,
        ReminderEntityConverter reminderEntityConverter,
        DailyLogEntityConverter dailyLogEntityConverter
    ) {
        this.petPersistenceMapper = petPersistenceMapper;
        this.userPersistenceMapper = userPersistenceMapper;
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.petEntityConverter = petEntityConverter;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.reminderEntityConverter = reminderEntityConverter;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
    }

    public List<PetDetailResponse> listPets() {
        Long currentUserId = CurrentUserContext.requireUserId();
        return petPersistenceMapper.listPetsByUserId(currentUserId).stream()
            .map(petEntityConverter::toEntity)
            .map(petEntityConverter::toPetDetailResponse)
            .toList();
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
        int updatedRows = petPersistenceMapper.updatePetSnapshot(
            petId,
            currentUserId,
            request.petName(),
            request.petType(),
            request.breed(),
            request.gender(),
            request.birthday(),
            request.adoptDate(),
            petEntityConverter.toNeuterStatusValue(request.neuterStatus()),
            request.avatarAssetId()
        );
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petEntityConverter.toPetDetailResponse(requireAccessiblePet(petId));
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

    private PetProfileEntity requireAccessiblePet(Long petId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        PetProfileEntity petProfile =
            petEntityConverter.toEntity(petPersistenceMapper.findAccessiblePetById(currentUserId, petId));
        if (petProfile == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petProfile;
    }
}
