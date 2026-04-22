package com.petlife.server.modules.user.persistence.dataobject;

/**
 * 用户资料数据对象。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param avatarUrl 头像地址
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param currentPetId 当前宠物 ID
 */
public record UserProfileDataObject(
    Long userId,
    String mobile,
    String nickname,
    String avatarUrl,
    String cityCode,
    String cityName,
    Long currentPetId
) {
}
