package com.petlife.server.modules.user.persistence.dataobject;

/**
 * 用户设置数据对象。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param cityCode 城市编码
 * @param cityName 城市名称
 * @param currentPetId 当前宠物 ID
 * @param notificationSwitch 通知总开关
 * @param privacyLevel 隐私级别
 */
public record UserSettingsDataObject(
    Long userId,
    String mobile,
    String nickname,
    String cityCode,
    String cityName,
    Long currentPetId,
    Integer notificationSwitch,
    String privacyLevel
) {
}
