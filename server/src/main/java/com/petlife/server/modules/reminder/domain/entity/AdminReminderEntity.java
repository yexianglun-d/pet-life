package com.petlife.server.modules.reminder.domain.entity;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;

/**
 * 后台提醒聚合实体。
 */
public final class AdminReminderEntity {

    private final ReminderEntity reminder;
    private final AdminPetContextEntity petContext;
    private final AdminUserContextEntity handlerContext;
    private final AdminReminderSourceEntity sourceContext;

    public AdminReminderEntity(
        ReminderEntity reminder,
        AdminPetContextEntity petContext,
        AdminUserContextEntity handlerContext,
        AdminReminderSourceEntity sourceContext
    ) {
        this.reminder = reminder;
        this.petContext = petContext;
        this.handlerContext = handlerContext;
        this.sourceContext = sourceContext;
    }

    public ReminderEntity getReminder() {
        return reminder;
    }

    public AdminPetContextEntity getPetContext() {
        return petContext;
    }

    public AdminUserContextEntity getHandlerContext() {
        return handlerContext;
    }

    public AdminReminderSourceEntity getSourceContext() {
        return sourceContext;
    }
}
