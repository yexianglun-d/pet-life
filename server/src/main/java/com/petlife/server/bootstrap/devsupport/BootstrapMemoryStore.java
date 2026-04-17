package com.petlife.server.bootstrap.devsupport;

import com.petlife.server.bootstrap.devsupport.model.DevFamilySummary;
import com.petlife.server.bootstrap.devsupport.model.DevHealthRecord;
import com.petlife.server.bootstrap.devsupport.model.DevDailyLog;
import com.petlife.server.bootstrap.devsupport.model.DevPetProfile;
import com.petlife.server.bootstrap.devsupport.model.DevReminder;
import com.petlife.server.bootstrap.devsupport.model.DevUserProfile;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * 开发期内存存储适配器。
 *
 * <p>当前阶段的目标是尽快打通主链路接口、页面和交互联调，因此这里采用线程安全的内存适配器承接
 * 用户、家庭和宠物数据。后续切换数据库时，优先保持 service 与 controller 契约不变，
 * 再把该类替换为正式持久化实现。</p>
 */
@Component
public class BootstrapMemoryStore {

    private static final long DEFAULT_CURRENT_PET_ID = 10001L;
    private static final long DEFAULT_LAST_PET_ID = 10002L;
    private static final long DEFAULT_LAST_HEALTH_RECORD_ID = 30002L;
    private static final long DEFAULT_LAST_REMINDER_ID = 40003L;
    private static final long DEFAULT_LAST_DAILY_LOG_ID = 50002L;

    private final AtomicReference<DevUserProfile> userReference;
    private final AtomicReference<DevFamilySummary> familyReference;
    private final AtomicLong petIdSequence;
    private final AtomicLong healthRecordIdSequence;
    private final AtomicLong reminderIdSequence;
    private final AtomicLong dailyLogIdSequence;
    private final ConcurrentMap<Long, DevPetProfile> petStore;
    private final ConcurrentMap<Long, DevHealthRecord> healthRecordStore;
    private final ConcurrentMap<Long, DevReminder> reminderStore;
    private final ConcurrentMap<Long, DevDailyLog> dailyLogStore;
    private final ConcurrentMap<String, Long> accessTokenStore;

    public BootstrapMemoryStore() {
        this.petIdSequence = new AtomicLong(DEFAULT_LAST_PET_ID);
        this.healthRecordIdSequence = new AtomicLong(DEFAULT_LAST_HEALTH_RECORD_ID);
        this.reminderIdSequence = new AtomicLong(DEFAULT_LAST_REMINDER_ID);
        this.dailyLogIdSequence = new AtomicLong(DEFAULT_LAST_DAILY_LOG_ID);
        this.petStore = new ConcurrentHashMap<>();
        this.healthRecordStore = new ConcurrentHashMap<>();
        this.reminderStore = new ConcurrentHashMap<>();
        this.dailyLogStore = new ConcurrentHashMap<>();
        this.accessTokenStore = new ConcurrentHashMap<>();
        this.userReference = new AtomicReference<>();
        this.familyReference = new AtomicReference<>();
        reset();
    }

    public DevUserProfile getCurrentUser() {
        return userReference.get();
    }

    public DevFamilySummary getFamilySummary() {
        return familyReference.get();
    }

    public String issueAccessToken(Long userId) {
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        accessTokenStore.put(accessToken, userId);
        return accessToken;
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Optional<Long> findUserIdByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(accessTokenStore.get(accessToken));
    }

    public List<DevPetProfile> listPets() {
        return petStore.values().stream()
            .sorted(Comparator.comparing(DevPetProfile::petId))
            .toList();
    }

    public DevPetProfile getPet(Long petId) {
        DevPetProfile petProfile = petStore.get(petId);
        if (petProfile == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
        return petProfile;
    }

    public DevUserProfile updateCurrentPet(Long petId) {
        getPet(petId);

        return userReference.updateAndGet(existingUser -> new DevUserProfile(
            existingUser.userId(),
            existingUser.mobile(),
            existingUser.nickname(),
            existingUser.cityCode(),
            existingUser.cityName(),
            petId
        ));
    }

    public DevPetProfile createPet(
        String petName,
        String petType,
        String breed,
        String gender,
        LocalDate birthday,
        LocalDate adoptDate,
        String neuterStatus,
        String avatarUrl
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        long petId = petIdSequence.incrementAndGet();
        DevPetProfile petProfile = new DevPetProfile(
            petId,
            petName,
            petType,
            breed,
            gender,
            birthday,
            adoptDate,
            neuterStatus,
            avatarUrl,
            now,
            now
        );
        petStore.put(petId, petProfile);
        return petProfile;
    }

    public DevPetProfile updatePet(
        Long petId,
        String petName,
        String petType,
        String breed,
        String gender,
        LocalDate birthday,
        LocalDate adoptDate,
        String neuterStatus,
        String avatarUrl
    ) {
        return petStore.compute(petId, (ignoredPetId, existingPet) -> {
            if (existingPet == null) {
                throw new BusinessException(ResponseCode.PET_NOT_FOUND);
            }

            return new DevPetProfile(
                existingPet.petId(),
                petName == null ? existingPet.petName() : petName,
                petType == null ? existingPet.petType() : petType,
                breed == null ? existingPet.breed() : breed,
                gender == null ? existingPet.gender() : gender,
                birthday == null ? existingPet.birthday() : birthday,
                adoptDate == null ? existingPet.adoptDate() : adoptDate,
                neuterStatus == null ? existingPet.neuterStatus() : neuterStatus,
                avatarUrl == null ? existingPet.avatarUrl() : avatarUrl,
                existingPet.createdAt(),
                OffsetDateTime.now()
            );
        });
    }

    public List<DevHealthRecord> listHealthRecords(Long petId) {
        getPet(petId);
        return healthRecordStore.values().stream()
            .filter(record -> record.petId().equals(petId))
            .sorted(Comparator.comparing(DevHealthRecord::occurredAt).reversed())
            .toList();
    }

    public DevHealthRecord createHealthRecord(
        Long petId,
        String recordType,
        String title,
        String value,
        String unit,
        OffsetDateTime occurredAt,
        String notes
    ) {
        getPet(petId);
        long healthRecordId = healthRecordIdSequence.incrementAndGet();
        OffsetDateTime now = OffsetDateTime.now();
        DevHealthRecord healthRecord = new DevHealthRecord(
            healthRecordId,
            petId,
            recordType,
            title,
            value,
            unit,
            occurredAt == null ? now : occurredAt,
            notes,
            now
        );
        healthRecordStore.put(healthRecordId, healthRecord);
        return healthRecord;
    }

    public List<DevReminder> listReminders(Long petId) {
        getPet(petId);
        return reminderStore.values().stream()
            .filter(reminder -> reminder.petId().equals(petId))
            .sorted(Comparator.comparing(DevReminder::dueAt))
            .toList();
    }

    public DevReminder createReminder(
        Long petId,
        String reminderType,
        String title,
        OffsetDateTime dueAt,
        String notes
    ) {
        getPet(petId);
        long reminderId = reminderIdSequence.incrementAndGet();
        OffsetDateTime now = OffsetDateTime.now();
        DevReminder reminder = new DevReminder(
            reminderId,
            petId,
            reminderType,
            title,
            dueAt == null ? now.plusDays(1) : dueAt,
            "pending",
            notes,
            null,
            now
        );
        reminderStore.put(reminderId, reminder);
        return reminder;
    }

    public DevReminder completeReminder(Long petId, Long reminderId) {
        getPet(petId);
        return reminderStore.compute(reminderId, (ignoredReminderId, existingReminder) -> {
            if (existingReminder == null || !existingReminder.petId().equals(petId)) {
                throw new BusinessException(ResponseCode.REMINDER_NOT_FOUND);
            }

            if ("completed".equals(existingReminder.status())) {
                return existingReminder;
            }

            return new DevReminder(
                existingReminder.reminderId(),
                existingReminder.petId(),
                existingReminder.reminderType(),
                existingReminder.title(),
                existingReminder.dueAt(),
                "completed",
                existingReminder.notes(),
                OffsetDateTime.now(),
                existingReminder.createdAt()
            );
        });
    }

    public List<DevDailyLog> listDailyLogs(Long petId) {
        getPet(petId);
        return dailyLogStore.values().stream()
            .filter(dailyLog -> dailyLog.petId().equals(petId))
            .sorted(Comparator.comparing(DevDailyLog::happenedAt).reversed())
            .toList();
    }

    public DevDailyLog createDailyLog(
        Long petId,
        String content,
        List<String> tags,
        String visibility,
        OffsetDateTime happenedAt
    ) {
        getPet(petId);
        long dailyLogId = dailyLogIdSequence.incrementAndGet();
        OffsetDateTime now = OffsetDateTime.now();
        DevDailyLog dailyLog = new DevDailyLog(
            dailyLogId,
            petId,
            content,
            List.copyOf(tags),
            visibility == null ? "private" : visibility,
            happenedAt == null ? now : happenedAt,
            now
        );
        dailyLogStore.put(dailyLogId, dailyLog);
        return dailyLog;
    }

    /**
     * 重置开发期内存基线数据。
     *
     * <p>该方法用于保障本地联调和自动化测试都基于同一套初始状态启动，避免单例内存存储在多次请求、
     * 多条测试用例之间出现状态污染。</p>
     */
    public synchronized void reset() {
        petStore.clear();
        healthRecordStore.clear();
        reminderStore.clear();
        dailyLogStore.clear();
        accessTokenStore.clear();
        petIdSequence.set(DEFAULT_LAST_PET_ID);
        healthRecordIdSequence.set(DEFAULT_LAST_HEALTH_RECORD_ID);
        reminderIdSequence.set(DEFAULT_LAST_REMINDER_ID);
        dailyLogIdSequence.set(DEFAULT_LAST_DAILY_LOG_ID);
        seedPets();
        seedHealthRecords();
        seedReminders();
        seedDailyLogs();
        userReference.set(new DevUserProfile(10001L, "13800000000", "Momo", "310100", "上海", DEFAULT_CURRENT_PET_ID));
        familyReference.set(new DevFamilySummary(20001L, "Momo Family", 2, "owner"));
    }

    private void seedPets() {
        OffsetDateTime seedTime = OffsetDateTime.now().minusDays(14);
        petStore.put(10001L, new DevPetProfile(
            10001L,
            "Momo",
            "cat",
            "British Shorthair",
            "female",
            LocalDate.of(2023, 5, 20),
            LocalDate.of(2023, 8, 1),
            "completed",
            null,
            seedTime,
            seedTime
        ));
        petStore.put(10002L, new DevPetProfile(
            10002L,
            "Dodo",
            "dog",
            "Corgi",
            "male",
            LocalDate.of(2022, 11, 2),
            LocalDate.of(2023, 1, 16),
            "pending",
            null,
            seedTime,
            seedTime
        ));
    }

    private void seedHealthRecords() {
        OffsetDateTime now = OffsetDateTime.now();
        healthRecordStore.put(30001L, new DevHealthRecord(
            30001L,
            10001L,
            "vaccine",
            "三联加强针",
            null,
            null,
            now.minusDays(7),
            "接种后精神状态稳定",
            now.minusDays(7)
        ));
        healthRecordStore.put(30002L, new DevHealthRecord(
            30002L,
            10001L,
            "weight",
            "体重复查",
            "4.3",
            "kg",
            now.minusDays(2),
            "饮食正常，建议继续观察活动量",
            now.minusDays(2)
        ));
    }

    private void seedReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        reminderStore.put(40001L, new DevReminder(
            40001L,
            10001L,
            "deworming",
            "体内驱虫提醒",
            now.plusDays(1),
            "pending",
            "饭后执行，注意观察食欲",
            null,
            now.minusDays(6)
        ));
        reminderStore.put(40002L, new DevReminder(
            40002L,
            10001L,
            "physical_exam",
            "年度体检预约",
            now.plusDays(4),
            "pending",
            "优先选择周六上午时段",
            null,
            now.minusDays(5)
        ));
        reminderStore.put(40003L, new DevReminder(
            40003L,
            10002L,
            "bath",
            "洗护预约",
            now.plusDays(2),
            "pending",
            "长毛修剪同步处理",
            null,
            now.minusDays(4)
        ));
    }

    private void seedDailyLogs() {
        OffsetDateTime now = OffsetDateTime.now();
        dailyLogStore.put(50001L, new DevDailyLog(
            50001L,
            10001L,
            "今天追着逗猫棒跑了十分钟，状态很活跃。",
            List.of("玩耍", "活跃"),
            "family",
            now.minusHours(10),
            now.minusHours(10)
        ));
        dailyLogStore.put(50002L, new DevDailyLog(
            50002L,
            10001L,
            "午睡时抱着小毯子，拍到了很可爱的照片。",
            List.of("睡觉", "照片"),
            "public",
            now.minusDays(1),
            now.minusDays(1)
        ));
    }
}
