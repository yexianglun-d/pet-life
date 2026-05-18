package com.petlife.server.modules.admin.persistence.dataobject;

/**
 * 后台登录会话数据对象。
 */
public record AdminSessionDataObject(
    Long sessionId,
    Long adminAccountId
) {
}
