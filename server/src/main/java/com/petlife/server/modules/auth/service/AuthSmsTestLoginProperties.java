package com.petlife.server.modules.auth.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码测试登录配置。
 *
 * <p>该配置只用于无真实短信供应商的测试环境。命中白名单时仍必须先生成验证码记录，
 * 并且只落库 hash 与 salt，避免恢复明文验证码或登录绕过。</p>
 */
@Component
@ConfigurationProperties(prefix = "petlife.auth.sms.test-login")
public class AuthSmsTestLoginProperties {

    private boolean enabled;
    private List<String> mobileWhitelist = List.of();
    private String code;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getMobileWhitelist() {
        return mobileWhitelist;
    }

    public void setMobileWhitelist(List<String> mobileWhitelist) {
        this.mobileWhitelist = mobileWhitelist == null ? List.of() : mobileWhitelist;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isEnabledForMobile(String mobile) {
        if (!enabled || mobile == null || mobile.isBlank()) {
            return false;
        }
        return mobileWhitelist.stream()
            .filter(candidate -> candidate != null && !candidate.isBlank())
            .map(String::trim)
            .anyMatch(mobile::equals);
    }

    public String normalizedCode() {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }
}
