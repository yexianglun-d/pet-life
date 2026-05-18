package com.petlife.server.modules.dailylog.domain.entity;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;

/**
 * 后台萌宠日常聚合实体。
 */
public final class AdminDailyLogEntity {

    private final DailyLogEntity dailyLog;
    private final AdminPetContextEntity petContext;
    private final AdminUserContextEntity authorContext;

    public AdminDailyLogEntity(
        DailyLogEntity dailyLog,
        AdminPetContextEntity petContext,
        AdminUserContextEntity authorContext
    ) {
        this.dailyLog = dailyLog;
        this.petContext = petContext;
        this.authorContext = authorContext;
    }

    public DailyLogEntity getDailyLog() {
        return dailyLog;
    }

    public AdminPetContextEntity getPetContext() {
        return petContext;
    }

    public AdminUserContextEntity getAuthorContext() {
        return authorContext;
    }
}
