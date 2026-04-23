package com.petlife.server.modules.pet.dto.request;

/**
 * 宠物归档请求。
 *
 * @param archiveStatus 归档状态，仅支持 memorial 或 rehomed
 */
public record ArchivePetRequest(
    String archiveStatus
) {
}
