package com.petlife.server.modules.admin.domain.entity;

/**
 * 后台操作上下文。
 *
 * <p>后台写操作审计需要稳定记录操作者、客户端 IP 和 User-Agent，避免业务服务直接依赖 Servlet API。</p>
 */
public record AdminOperationContext(
    String operatorId,
    String ipAddress,
    String userAgent
) {
}
