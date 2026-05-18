package com.petlife.server.modules.health.domain.entity;

import com.petlife.server.modules.admin.domain.entity.AdminPetContextEntity;
import com.petlife.server.modules.admin.domain.entity.AdminUserContextEntity;

/**
 * 后台健康记录聚合实体。
 */
public final class AdminHealthRecordEntity {

    private final HealthRecordEntity healthRecord;
    private final AdminPetContextEntity petContext;
    private final AdminUserContextEntity operatorContext;

    public AdminHealthRecordEntity(
        HealthRecordEntity healthRecord,
        AdminPetContextEntity petContext,
        AdminUserContextEntity operatorContext
    ) {
        this.healthRecord = healthRecord;
        this.petContext = petContext;
        this.operatorContext = operatorContext;
    }

    public HealthRecordEntity getHealthRecord() {
        return healthRecord;
    }

    public AdminPetContextEntity getPetContext() {
        return petContext;
    }

    public AdminUserContextEntity getOperatorContext() {
        return operatorContext;
    }
}
