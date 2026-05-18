package com.petlife.server.modules.user.persistence.command;

/**
 * 用户城市更新命令。
 */
public class UpdateUserCityCommand {

    private Long userId;
    private String cityCode;
    private String cityName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
}
