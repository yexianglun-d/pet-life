package com.petlife.server.bootstrap.devsupport.model;

/**
 * 开发期用户模型。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param currentPetId 当前宠物 ID
 */
public record DevUserProfile(
    Long userId,
    String mobile,
    String nickname,
    String cityCode,
    String cityName,
    Long currentPetId
) {
}
