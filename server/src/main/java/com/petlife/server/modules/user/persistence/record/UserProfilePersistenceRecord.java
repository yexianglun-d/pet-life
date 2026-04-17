package com.petlife.server.modules.user.persistence.record;

/**
 * 用户资料持久化记录。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param avatarUrl 头像地址
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param currentPetId 当前宠物 ID
 */
public record UserProfilePersistenceRecord(
    Long userId,
    String mobile,
    String nickname,
    String avatarUrl,
    String cityCode,
    String cityName,
    Long currentPetId
) {
}
