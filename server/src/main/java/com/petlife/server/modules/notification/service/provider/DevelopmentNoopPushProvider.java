package com.petlife.server.modules.notification.service.provider;

import org.springframework.stereotype.Component;

/**
 * 开发期 Push 供应商。
 *
 * <p>当前不接 APNs/FCM/厂商通道，因此该 provider 只参与任务归属，不执行真实投递。</p>
 */
@Component
public class DevelopmentNoopPushProvider implements PushProvider {

    public static final String PROVIDER_CODE = "dev_noop";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean dispatchEnabled() {
        return false;
    }
}
