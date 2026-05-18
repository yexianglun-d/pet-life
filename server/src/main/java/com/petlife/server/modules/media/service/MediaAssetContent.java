package com.petlife.server.modules.media.service;

import com.petlife.server.modules.media.domain.entity.MediaAssetEntity;
import org.springframework.core.io.Resource;

/**
 * 媒体文件内容。
 *
 * @param mediaAsset 媒体资产
 * @param resource 文件资源
 */
public record MediaAssetContent(
    MediaAssetEntity mediaAsset,
    Resource resource
) {
}
