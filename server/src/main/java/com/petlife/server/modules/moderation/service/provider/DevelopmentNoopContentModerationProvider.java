package com.petlife.server.modules.moderation.service.provider;

import org.springframework.stereotype.Component;

/**
 * 开发期审核供应商。
 *
 * <p>该实现只确认任务已被服务端记录，不返回通过结论，避免把未接入第三方审核误认为真实审核成功。</p>
 */
@Component
public class DevelopmentNoopContentModerationProvider implements ContentModerationProvider {

    public static final String PROVIDER_CODE = "dev_noop";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ModerationSubmissionResult submit(ModerationSubmissionRequest request) {
        return new ModerationSubmissionResult(
            "pending",
            "{\"provider_status\":\"not_dispatched\"}",
            "[]",
            null
        );
    }
}
