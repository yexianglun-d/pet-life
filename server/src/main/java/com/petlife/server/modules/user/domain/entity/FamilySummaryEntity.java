package com.petlife.server.modules.user.domain.entity;

/**
 * 家庭摘要实体。
 *
 * <p>该实体用于表达当前用户在某个家庭下的归属关系和角色信息，
 * 是登录态、当前用户信息和家庭共养能力的共用输入。</p>
 */
public final class FamilySummaryEntity {

    private final Long familyId;
    private final String familyName;
    private final Integer memberCount;
    private final String role;

    public FamilySummaryEntity(
        Long familyId,
        String familyName,
        Integer memberCount,
        String role
    ) {
        this.familyId = familyId;
        this.familyName = familyName;
        this.memberCount = memberCount;
        this.role = role;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public String getRole() {
        return role;
    }
}
