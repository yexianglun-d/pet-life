package com.petlife.server.modules.pet.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevDailyLog;
import com.petlife.server.bootstrap.devsupport.model.DevHealthRecord;
import com.petlife.server.bootstrap.devsupport.model.DevPetProfile;
import com.petlife.server.bootstrap.devsupport.model.DevReminder;
import com.petlife.server.modules.pet.dto.request.CreatePetRequest;
import com.petlife.server.modules.pet.dto.request.UpdatePetRequest;
import com.petlife.server.modules.pet.dto.response.PetDetailResponse;
import com.petlife.server.modules.pet.dto.response.PetSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 宠物应用服务。
 *
 * <p>当前阶段的聚合内容先由服务层直接组装固定示例数据，
 * 目的是为移动端与后台联调提供稳定输出。后续接入健康、日常和提醒模块后，
 * 再替换为真实聚合查询。</p>
 */
@Service
public class PetApplicationService {

    private final BootstrapMemoryStore bootstrapMemoryStore;

    public PetApplicationService(BootstrapMemoryStore bootstrapMemoryStore) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
    }

    public List<PetDetailResponse> listPets() {
        return bootstrapMemoryStore.listPets().stream()
            .map(this::toPetDetailResponse)
            .toList();
    }

    public PetDetailResponse createPet(CreatePetRequest request) {
        DevPetProfile petProfile = bootstrapMemoryStore.createPet(
            request.petName(),
            request.petType(),
            request.breed(),
            request.gender(),
            request.birthday(),
            request.adoptDate(),
            request.neuterStatus(),
            request.avatarAssetId()
        );
        return toPetDetailResponse(petProfile);
    }

    public PetDetailResponse getPet(Long petId) {
        return toPetDetailResponse(bootstrapMemoryStore.getPet(petId));
    }

    public PetDetailResponse updatePet(Long petId, UpdatePetRequest request) {
        DevPetProfile updatedPet = bootstrapMemoryStore.updatePet(
            petId,
            request.petName(),
            request.petType(),
            request.breed(),
            request.gender(),
            request.birthday(),
            request.adoptDate(),
            request.neuterStatus(),
            request.avatarAssetId()
        );
        return toPetDetailResponse(updatedPet);
    }

    public PetSummaryResponse getPetSummary(Long petId) {
        PetDetailResponse petDetail = getPet(petId);
        List<DevHealthRecord> healthRecords = bootstrapMemoryStore.listHealthRecords(petId);
        List<DevReminder> reminders = bootstrapMemoryStore.listReminders(petId);
        List<DevDailyLog> dailyLogs = bootstrapMemoryStore.listDailyLogs(petId);

        return new PetSummaryResponse(
            petDetail,
            Math.toIntExact(reminders.stream().filter(reminder -> "pending".equals(reminder.status())).count()),
            healthRecords.stream().limit(3).map(DevHealthRecord::title).toList(),
            dailyLogs.stream().limit(3).map(DevDailyLog::content).toList()
        );
    }

    private PetDetailResponse toPetDetailResponse(DevPetProfile petProfile) {
        return new PetDetailResponse(
            String.valueOf(petProfile.petId()),
            petProfile.petName(),
            petProfile.petType(),
            petProfile.breed(),
            petProfile.gender(),
            petProfile.birthday(),
            petProfile.adoptDate(),
            petProfile.neuterStatus(),
            petProfile.avatarUrl(),
            petProfile.createdAt(),
            petProfile.updatedAt()
        );
    }
}
