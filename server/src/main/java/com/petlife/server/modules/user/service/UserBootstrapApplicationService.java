package com.petlife.server.modules.user.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.family.persistence.FamilyPersistenceMapper;
import com.petlife.server.modules.family.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.FamilySummaryEntity;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户基础数据补齐应用服务。
 *
 * <p>登录、成员移除等链路都会影响“主家庭 + 当前宠物”这组基础上下文。
 * 这里统一负责补齐家庭、默认宠物和当前宠物指针，避免不同业务各自复制一套脆弱逻辑。</p>
 */
@Service
public class UserBootstrapApplicationService {

    private final UserPersistenceMapper userPersistenceMapper;
    private final FamilyPersistenceMapper familyPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final PetEntityConverter petEntityConverter;

    public UserBootstrapApplicationService(
        UserPersistenceMapper userPersistenceMapper,
        FamilyPersistenceMapper familyPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        UserEntityConverter userEntityConverter,
        PetEntityConverter petEntityConverter
    ) {
        this.userPersistenceMapper = userPersistenceMapper;
        this.familyPersistenceMapper = familyPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.petEntityConverter = petEntityConverter;
    }

    @Transactional
    public FamilySummaryEntity ensurePrimaryFamilyAndCurrentPet(Long userId) {
        UserProfileEntity userProfile = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(userId));
        if (userProfile == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        userPersistenceMapper.insertUserSettingsIfAbsent(userId);
        FamilySummaryEntity familySummary = ensurePrimaryFamily(userProfile);
        ensureCurrentPet(userProfile, familySummary);
        return userEntityConverter.toEntity(userPersistenceMapper.findPrimaryFamilySummaryByUserId(userId));
    }

    private FamilySummaryEntity ensurePrimaryFamily(UserProfileEntity userProfile) {
        FamilySummaryEntity existingFamily =
            userEntityConverter.toEntity(userPersistenceMapper.findPrimaryFamilySummaryByUserId(userProfile.getUserId()));
        if (existingFamily != null) {
            return existingFamily;
        }

        CreateFamilyCommand command = new CreateFamilyCommand();
        command.setOwnerUserId(userProfile.getUserId());
        command.setFamilyName(userProfile.getNickname() + "的家庭");
        familyPersistenceMapper.insertFamily(command);
        familyPersistenceMapper.insertFamilyMember(command.getId(), userProfile.getUserId(), "owner");
        return userEntityConverter.toEntity(userPersistenceMapper.findPrimaryFamilySummaryByUserId(userProfile.getUserId()));
    }

    /**
     * 当前宠物既可能缺失，也可能指向已经无权访问的旧家庭宠物。
     *
     * <p>这里统一用“当前可访问宠物集合”作为真实来源：
     * 若没有任何可访问宠物则创建默认宠物；若存在宠物但当前指针无效，则回落到第一只可访问宠物。</p>
     */
    private void ensureCurrentPet(
        UserProfileEntity userProfile,
        FamilySummaryEntity familySummary
    ) {
        List<PetProfileEntity> accessiblePets = petPersistenceMapper.listPetsByUserId(userProfile.getUserId()).stream()
            .map(petEntityConverter::toEntity)
            .toList();
        if (accessiblePets.isEmpty()) {
            if (petPersistenceMapper.existsPetHistoryByUserId(userProfile.getUserId())) {
                userPersistenceMapper.updateCurrentPet(userProfile.getUserId(), null);
                return;
            }
            CreatePetCommand command = new CreatePetCommand();
            command.setFamilyId(familySummary.getFamilyId());
            command.setOwnerUserId(userProfile.getUserId());
            command.setPetName("Momo".equals(userProfile.getNickname()) ? "Momo" : "宠物宝宝");
            command.setPetType("cat");
            command.setBreed("British Shorthair");
            command.setGender("female");
            command.setBirthday(LocalDate.of(2023, 5, 20));
            command.setAdoptDate(LocalDate.of(2023, 8, 1));
            command.setNeuterStatus(1);
            petPersistenceMapper.insertPet(command);
            userPersistenceMapper.updateCurrentPet(userProfile.getUserId(), command.getId());
            return;
        }

        boolean hasAccessibleCurrentPet = userProfile.getCurrentPetId() != null
            && accessiblePets.stream().anyMatch(pet -> pet.getPetId().equals(userProfile.getCurrentPetId()));
        if (!hasAccessibleCurrentPet) {
            userPersistenceMapper.updateCurrentPet(userProfile.getUserId(), accessiblePets.get(0).getPetId());
        }
    }

    /**
     * 宠物被归档、删除或移除访问权限后，所有引用它的成员都必须统一重建当前宠物上下文。
     *
     * <p>否则 `/me`、首页和宠物页都会读到悬空的 current_pet_id，导致整条用户主链路报错。</p>
     */
    @Transactional
    public void rebuildCurrentPetContextForPet(Long petId) {
        for (Long userId : petPersistenceMapper.listUserIdsByCurrentPetId(petId)) {
            ensurePrimaryFamilyAndCurrentPet(userId);
        }
    }
}
