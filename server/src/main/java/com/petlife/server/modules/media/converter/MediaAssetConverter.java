package com.petlife.server.modules.media.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.media.domain.entity.MediaAssetEntity;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import com.petlife.server.modules.media.persistence.dataobject.MediaAssetDataObject;
import org.springframework.stereotype.Component;

/**
 * 媒体资产转换器。
 */
@Component
public class MediaAssetConverter {

    public MediaAssetEntity toEntity(MediaAssetDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new MediaAssetEntity(
            dataObject.mediaAssetId(),
            dataObject.uploaderUserId(),
            dataObject.bizType(),
            dataObject.mediaType(),
            dataObject.fileName(),
            dataObject.objectKey(),
            dataObject.bucketName(),
            dataObject.cdnUrl(),
            dataObject.contentType(),
            dataObject.fileSize(),
            dataObject.fileHash(),
            dataObject.uploadStatus(),
            dataObject.reviewStatus(),
            dataObject.completedAt(),
            dataObject.createdAt()
        );
    }

    public MediaAssetResponse toResponse(MediaAssetEntity mediaAsset) {
        String accessUrl = mediaAsset.getCdnUrl();
        if (accessUrl == null || accessUrl.isBlank()) {
            accessUrl = "/api/v1/media-assets/%s/content".formatted(mediaAsset.getMediaAssetId());
        }
        return new MediaAssetResponse(
            String.valueOf(mediaAsset.getMediaAssetId()),
            mediaAsset.getBizType(),
            mediaAsset.getMediaType(),
            mediaAsset.getFileName(),
            mediaAsset.getContentType(),
            mediaAsset.getFileSize(),
            mediaAsset.getFileHash(),
            mediaAsset.getUploadStatus(),
            mediaAsset.getReviewStatus(),
            accessUrl,
            DateTimeConverters.toOffsetDateTime(mediaAsset.getCompletedAt()),
            DateTimeConverters.toOffsetDateTime(mediaAsset.getCreatedAt())
        );
    }
}
