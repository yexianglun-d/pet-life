package com.petlife.server.modules.home.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.dailylog.converter.DailyLogEntityConverter;
import com.petlife.server.modules.dailylog.domain.entity.DailyLogEntity;
import com.petlife.server.modules.dailylog.persistence.DailyLogPersistenceMapper;
import com.petlife.server.modules.health.converter.HealthRecordEntityConverter;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.persistence.HealthRecordPersistenceMapper;
import com.petlife.server.modules.home.converter.HomeReportConverter;
import com.petlife.server.modules.home.domain.entity.HomePetReportEntity;
import com.petlife.server.modules.home.dto.response.HomePetReportResponse;
import com.petlife.server.modules.pet.converter.PetEntityConverter;
import com.petlife.server.modules.pet.domain.entity.PetProfileEntity;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.reminder.converter.ReminderEntityConverter;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.reminder.persistence.ReminderPersistenceMapper;
import com.petlife.server.modules.user.converter.UserEntityConverter;
import com.petlife.server.modules.user.domain.entity.UserProfileEntity;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.service.UserBootstrapApplicationService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 首页应用服务。
 *
 * <p>首页周期报告以“当前宠物”为唯一主轴，聚合提醒、健康和日常记录，
 * 让周报与月报都基于服务端统一口径输出，避免不同客户端各自统计出不一致结果。</p>
 */
@Service
public class HomeApplicationService {

    private static final int WEEKLY_DAYS = 7;
    private static final int MONTHLY_DAYS = 30;
    private static final int RECENT_LIMIT = 5;

    private final UserPersistenceMapper userPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final ReminderPersistenceMapper reminderPersistenceMapper;
    private final HealthRecordPersistenceMapper healthRecordPersistenceMapper;
    private final DailyLogPersistenceMapper dailyLogPersistenceMapper;
    private final UserEntityConverter userEntityConverter;
    private final PetEntityConverter petEntityConverter;
    private final ReminderEntityConverter reminderEntityConverter;
    private final HealthRecordEntityConverter healthRecordEntityConverter;
    private final DailyLogEntityConverter dailyLogEntityConverter;
    private final HomeReportConverter homeReportConverter;
    private final UserBootstrapApplicationService userBootstrapApplicationService;

    public HomeApplicationService(
        UserPersistenceMapper userPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        ReminderPersistenceMapper reminderPersistenceMapper,
        HealthRecordPersistenceMapper healthRecordPersistenceMapper,
        DailyLogPersistenceMapper dailyLogPersistenceMapper,
        UserEntityConverter userEntityConverter,
        PetEntityConverter petEntityConverter,
        ReminderEntityConverter reminderEntityConverter,
        HealthRecordEntityConverter healthRecordEntityConverter,
        DailyLogEntityConverter dailyLogEntityConverter,
        HomeReportConverter homeReportConverter,
        UserBootstrapApplicationService userBootstrapApplicationService
    ) {
        this.userPersistenceMapper = userPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.reminderPersistenceMapper = reminderPersistenceMapper;
        this.healthRecordPersistenceMapper = healthRecordPersistenceMapper;
        this.dailyLogPersistenceMapper = dailyLogPersistenceMapper;
        this.userEntityConverter = userEntityConverter;
        this.petEntityConverter = petEntityConverter;
        this.reminderEntityConverter = reminderEntityConverter;
        this.healthRecordEntityConverter = healthRecordEntityConverter;
        this.dailyLogEntityConverter = dailyLogEntityConverter;
        this.homeReportConverter = homeReportConverter;
        this.userBootstrapApplicationService = userBootstrapApplicationService;
    }

    public HomePetReportResponse getWeeklyReport() {
        return homeReportConverter.toResponse(buildPeriodReport("weekly", WEEKLY_DAYS));
    }

    public HomePetReportResponse getMonthlyReport() {
        return homeReportConverter.toResponse(buildPeriodReport("monthly", MONTHLY_DAYS));
    }

    private HomePetReportEntity buildPeriodReport(String reportType, int periodDays) {
        Long currentUserId = CurrentUserContext.requireUserId();
        userBootstrapApplicationService.ensurePrimaryFamilyAndCurrentPet(currentUserId);
        UserProfileEntity currentUser = userEntityConverter.toEntity(userPersistenceMapper.findUserProfileById(currentUserId));
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "当前用户不存在");
        }
        if (currentUser.getCurrentPetId() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前还没有宠物主档");
        }

        PetProfileEntity currentPet = petEntityConverter.toEntity(
            petPersistenceMapper.findAccessiblePetById(currentUserId, currentUser.getCurrentPetId())
        );
        if (currentPet == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }

        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.toLocalDate().minusDays(periodDays - 1L).atStartOfDay();

        List<ReminderEntity> reminders = reminderPersistenceMapper.listRemindersByPetId(currentPet.getPetId()).stream()
            .map(reminderEntityConverter::toEntity)
            .toList();
        List<HealthRecordEntity> healthRecords =
            healthRecordPersistenceMapper.listHealthRecordsByPetId(currentPet.getPetId()).stream()
                .map(healthRecordEntityConverter::toEntity)
                .filter(record -> isWithinWindow(record.getOccurredAt(), windowStart, windowEnd))
                .sorted(Comparator.comparing(HealthRecordEntity::getOccurredAt).reversed())
                .toList();
        List<DailyLogEntity> dailyLogs = dailyLogPersistenceMapper.listDailyLogsByPetId(currentPet.getPetId()).stream()
            .map(dailyLogEntityConverter::toEntity)
            .filter(log -> isWithinWindow(log.getHappenedAt(), windowStart, windowEnd))
            .sorted(Comparator.comparing(DailyLogEntity::getHappenedAt).reversed())
            .toList();

        int pendingReminderCount = (int) reminders.stream()
            .filter(reminder -> "pending".equals(reminder.getStatus()))
            .filter(reminder -> isWithinWindow(reminder.getDueAt(), windowStart, windowEnd))
            .count();
        int completedReminderCount = (int) reminders.stream()
            .filter(reminder -> "completed".equals(reminder.getStatus()))
            .filter(reminder -> isWithinWindow(reminder.getHandledAt(), windowStart, windowEnd))
            .count();
        int skippedReminderCount = (int) reminders.stream()
            .filter(reminder -> "skipped".equals(reminder.getStatus()))
            .filter(reminder -> isWithinWindow(reminder.getHandledAt(), windowStart, windowEnd))
            .count();

        int feedCount = countDailyLogTag(dailyLogs, "喂食");
        int waterCount = countDailyLogTag(dailyLogs, "饮水");
        int toiletCount = countDailyLogTag(dailyLogs, "排便");
        int weightRecordCount = (int) healthRecords.stream()
            .filter(record -> "weight".equals(record.getRecordType()))
            .count();
        int medicationRecordCount = (int) healthRecords.stream()
            .filter(record -> "medication".equals(record.getRecordType()))
            .count();
        int communitySyncCount = (int) dailyLogs.stream()
            .filter(DailyLogEntity::isSyncToCommunity)
            .count();

        List<ReminderEntity> recentReminders = reminders.stream()
            .filter(reminder -> isReminderRelevant(reminder, windowStart, windowEnd))
            .sorted(Comparator.comparing(this::resolveReminderDisplayTime).reversed())
            .limit(RECENT_LIMIT)
            .toList();

        return new HomePetReportEntity(
            reportType,
            currentPet,
            windowStart,
            windowEnd,
            pendingReminderCount,
            completedReminderCount,
            skippedReminderCount,
            healthRecords.size(),
            dailyLogs.size(),
            communitySyncCount,
            feedCount,
            waterCount,
            toiletCount,
            weightRecordCount,
            medicationRecordCount,
            buildHighlights(
                pendingReminderCount,
                completedReminderCount,
                healthRecords.size(),
                dailyLogs.size(),
                communitySyncCount,
                currentPet.getPetName()
            ),
            recentReminders,
            healthRecords.stream().limit(RECENT_LIMIT).toList(),
            dailyLogs.stream().limit(RECENT_LIMIT).toList()
        );
    }

    private boolean isReminderRelevant(ReminderEntity reminder, LocalDateTime windowStart, LocalDateTime windowEnd) {
        if ("pending".equals(reminder.getStatus())) {
            return isWithinWindow(reminder.getDueAt(), windowStart, windowEnd);
        }
        return isWithinWindow(reminder.getHandledAt(), windowStart, windowEnd);
    }

    private LocalDateTime resolveReminderDisplayTime(ReminderEntity reminder) {
        return reminder.getHandledAt() == null ? reminder.getDueAt() : reminder.getHandledAt();
    }

    private boolean isWithinWindow(LocalDateTime value, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return value != null && !value.isBefore(windowStart) && !value.isAfter(windowEnd);
    }

    private int countDailyLogTag(List<DailyLogEntity> dailyLogs, String tag) {
        return (int) dailyLogs.stream()
            .filter(dailyLog -> dailyLog.getTags().contains(tag))
            .count();
    }

    private List<String> buildHighlights(
        int pendingReminderCount,
        int completedReminderCount,
        int healthRecordCount,
        int dailyLogCount,
        int communitySyncCount,
        String petName
    ) {
        java.util.ArrayList<String> highlights = new java.util.ArrayList<>();
        if (pendingReminderCount > 0) {
            highlights.add("还有 " + pendingReminderCount + " 条提醒待处理，下一步照护重点已经很清楚。");
        }
        if (completedReminderCount > 0) {
            highlights.add("这段时间已经完成了 " + completedReminderCount + " 条提醒，照护节奏保持得不错。");
        }
        if (healthRecordCount > 0) {
            highlights.add("围绕 " + petName + " 留下了 " + healthRecordCount + " 条健康记录。");
        }
        if (dailyLogCount > 0) {
            highlights.add("这段时间记录了 " + dailyLogCount + " 条陪伴片段，生活痕迹在慢慢变完整。");
        }
        if (communitySyncCount > 0) {
            highlights.add("其中有 " + communitySyncCount + " 条内容同步到了社区。");
        }
        if (highlights.isEmpty()) {
            highlights.add("这一段时间还比较安静，可以从一次快捷记录开始，把陪伴重新接起来。");
        }
        return List.copyOf(highlights);
    }
}
