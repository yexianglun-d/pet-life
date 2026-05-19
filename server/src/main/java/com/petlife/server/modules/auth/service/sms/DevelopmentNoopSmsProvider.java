package com.petlife.server.modules.auth.service.sms;

import org.springframework.stereotype.Component;

/**
 * 开发期短信供应商占位实现。
 *
 * <p>当前不接入任何云厂商 SDK。本实现只表示“发送请求已被服务端接受并记录”，
 * 不保存、不返回、不打印验证码，避免重新引入明文验证码泄露。</p>
 */
@Component
public class DevelopmentNoopSmsProvider implements SmsProvider {

    private static final String PROVIDER_CODE = "dev_noop";
    private static final String SEND_STATUS_ACCEPTED = "accepted";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SmsSendResult sendVerificationCode(String mobile, String scene, String code) {
        return new SmsSendResult(SEND_STATUS_ACCEPTED, null);
    }
}
