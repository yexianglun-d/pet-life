package com.petlife.server.modules.moderation.service.provider;

/**
 * 内容审核供应商抽象。
 */
public interface ContentModerationProvider {

    String providerCode();

    ModerationSubmissionResult submit(ModerationSubmissionRequest request);
}
