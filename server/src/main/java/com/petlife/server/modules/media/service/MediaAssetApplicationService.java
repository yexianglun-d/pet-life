package com.petlife.server.modules.media.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.media.converter.MediaAssetConverter;
import com.petlife.server.modules.media.domain.entity.MediaAssetEntity;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import com.petlife.server.modules.media.persistence.MediaAssetPersistenceMapper;
import com.petlife.server.modules.media.persistence.command.CreateMediaAssetCommand;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体资产应用服务。
 */
@Service
public class MediaAssetApplicationService {

    private static final String UPLOAD_STATUS_UPLOADED = "uploaded";
    private static final String REVIEW_STATUS_PENDING = "pending_review";
    private static final Map<String, Set<String>> BIZ_MEDIA_TYPES = Map.of(
        "avatar", Set.of("image"),
        "health_report", Set.of("image", "file"),
        "daily_log", Set.of("image", "video"),
        "community", Set.of("image", "video"),
        "service_review", Set.of("image")
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov");

    private final MediaAssetPersistenceMapper mediaAssetPersistenceMapper;
    private final MediaAssetConverter mediaAssetConverter;
    private final MediaStorageService mediaStorageService;

    public MediaAssetApplicationService(
        MediaAssetPersistenceMapper mediaAssetPersistenceMapper,
        MediaAssetConverter mediaAssetConverter,
        MediaStorageService mediaStorageService
    ) {
        this.mediaAssetPersistenceMapper = mediaAssetPersistenceMapper;
        this.mediaAssetConverter = mediaAssetConverter;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public MediaAssetResponse uploadMediaAsset(String bizType, MultipartFile file) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedBizType = normalizeBizType(bizType);
        requireValidFile(file);
        String fileName = normalizeFileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType(), fileName);
        String mediaType = resolveMediaType(contentType, fileName);
        requireSupportedMediaType(normalizedBizType, mediaType);

        StoredMediaObject storedMediaObject =
            mediaStorageService.store(currentUserId, normalizedBizType, fileName, contentType, file);
        CreateMediaAssetCommand command = new CreateMediaAssetCommand();
        command.setUploaderUserId(currentUserId);
        command.setBizType(normalizedBizType);
        command.setMediaType(mediaType);
        command.setFileName(fileName);
        command.setObjectKey(storedMediaObject.objectKey());
        command.setBucketName(storedMediaObject.bucketName());
        command.setCdnUrl(storedMediaObject.accessUrl());
        command.setContentType(storedMediaObject.contentType());
        command.setFileSize(storedMediaObject.fileSize());
        command.setFileHash(storedMediaObject.fileHash());
        command.setUploadStatus(UPLOAD_STATUS_UPLOADED);
        command.setReviewStatus(REVIEW_STATUS_PENDING);
        command.setCompletedAt(LocalDateTime.now());
        mediaAssetPersistenceMapper.insertMediaAsset(command);

        return mediaAssetConverter.toResponse(requireMediaAsset(command.getId()));
    }

    public MediaAssetResponse getMediaAsset(Long mediaAssetId) {
        return mediaAssetConverter.toResponse(requireReadableMediaAsset(mediaAssetId));
    }

    public MediaAssetContent getMediaAssetContent(Long mediaAssetId) {
        MediaAssetEntity mediaAsset = requireReadableMediaAsset(mediaAssetId);
        return new MediaAssetContent(mediaAsset, mediaStorageService.loadAsResource(mediaAsset.getObjectKey()));
    }

    public List<MediaAssetResponse> listReadableMediaAssetResponses(Long userId, List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        return assetIds.stream()
            .map(this::normalizeAssetId)
            .filter(assetId -> assetId != null)
            .distinct()
            .map(assetId -> requireReadableMediaAsset(userId, Long.valueOf(assetId)))
            .map(mediaAssetConverter::toResponse)
            .toList();
    }

    public List<MediaAssetResponse> listUploadedMediaAssetResponses(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        return assetIds.stream()
            .map(this::normalizeAssetId)
            .filter(assetId -> assetId != null)
            .distinct()
            .map(assetId -> mediaAssetConverter.toEntity(
                mediaAssetPersistenceMapper.findUploadedAssetById(Long.valueOf(assetId))
            ))
            .filter(java.util.Objects::nonNull)
            .map(mediaAssetConverter::toResponse)
            .toList();
    }

    /**
     * 业务记录只允许引用当前用户已上传完成且业务类型匹配的资产。
     * 这样健康和日常不会沉淀无效 asset_id，也避免跨用户复用文件造成隐私越权。
     */
    public List<String> validateUsableAssetIds(
        Long userId,
        List<String> assetIds,
        String requiredBizType,
        Set<String> allowedMediaTypes
    ) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        String normalizedBizType = normalizeBizType(requiredBizType);
        Set<String> normalizedAssetIds = new LinkedHashSet<>();
        for (String assetId : assetIds) {
            String normalizedAssetId = normalizeAssetId(assetId);
            if (normalizedAssetId != null) {
                normalizedAssetIds.add(normalizedAssetId);
            }
        }
        for (String assetId : normalizedAssetIds) {
            MediaAssetEntity mediaAsset = mediaAssetConverter.toEntity(
                mediaAssetPersistenceMapper.findUploadedAssetByUserIdAndId(userId, Long.valueOf(assetId))
            );
            if (mediaAsset == null) {
                throw new BusinessException(ResponseCode.MEDIA_ASSET_NOT_FOUND, "媒体资产不存在或尚未上传完成");
            }
            if (!normalizedBizType.equals(mediaAsset.getBizType())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体资产业务类型不匹配");
            }
            if (!allowedMediaTypes.contains(mediaAsset.getMediaType())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体资产类型不支持当前业务");
            }
        }
        return List.copyOf(normalizedAssetIds);
    }

    private MediaAssetEntity requireMediaAsset(Long mediaAssetId) {
        MediaAssetEntity mediaAsset =
            mediaAssetConverter.toEntity(mediaAssetPersistenceMapper.findMediaAssetById(mediaAssetId));
        if (mediaAsset == null) {
            throw new BusinessException(ResponseCode.MEDIA_ASSET_NOT_FOUND);
        }
        return mediaAsset;
    }

    private MediaAssetEntity requireReadableMediaAsset(Long mediaAssetId) {
        return requireReadableMediaAsset(CurrentUserContext.requireUserId(), mediaAssetId);
    }

    /**
     * 读取权限既包含上传者本人，也包含已通过宠物记录获得访问权的家庭成员。
     * 健康附件和日常媒体都从业务记录反查授权，避免家庭共养场景下只返回 asset_id 却无法预览文件。
     */
    private MediaAssetEntity requireReadableMediaAsset(Long currentUserId, Long mediaAssetId) {
        MediaAssetEntity mediaAsset = mediaAssetConverter.toEntity(
            mediaAssetPersistenceMapper.findReadableUploadedAssetByUserIdAndId(currentUserId, mediaAssetId)
        );
        if (mediaAsset == null) {
            throw new BusinessException(ResponseCode.MEDIA_ASSET_NOT_FOUND);
        }
        return mediaAsset;
    }

    private String normalizeBizType(String bizType) {
        String normalizedBizType = bizType == null ? "" : bizType.trim();
        if (!BIZ_MEDIA_TYPES.containsKey(normalizedBizType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体业务类型不支持");
        }
        return normalizedBizType;
    }

    private void requireValidFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > mediaStorageService.getMaxFileSizeBytes()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "上传文件超过大小限制");
        }
    }

    private String normalizeFileName(String originalFilename) {
        String normalizedFilename = originalFilename == null ? "" : originalFilename.trim().replace('\\', '/');
        int slashIndex = normalizedFilename.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalizedFilename = normalizedFilename.substring(slashIndex + 1);
        }
        normalizedFilename = normalizedFilename.replaceAll("[\\r\\n\\t]", "");
        return normalizedFilename.isBlank() ? "uploaded-file" : normalizedFilename;
    }

    private String normalizeContentType(String contentType, String fileName) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase();
        if (normalizedContentType.isBlank() || "application/octet-stream".equals(normalizedContentType)) {
            String extension = resolveFileExtension(fileName);
            if ("pdf".equals(extension)) {
                return "application/pdf";
            }
            if (IMAGE_EXTENSIONS.contains(extension)) {
                return "image/" + ("jpg".equals(extension) ? "jpeg" : extension);
            }
            if ("mov".equals(extension)) {
                return "video/quicktime";
            }
            if (VIDEO_EXTENSIONS.contains(extension)) {
                return "video/" + extension;
            }
        }
        return normalizedContentType;
    }

    private String resolveMediaType(String contentType, String fileName) {
        if (contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType.startsWith("video/")) {
            return "video";
        }
        if ("application/pdf".equals(contentType)) {
            return "file";
        }
        String extension = resolveFileExtension(fileName);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return "video";
        }
        if ("pdf".equals(extension)) {
            return "file";
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "仅支持图片、视频或 PDF 文件");
    }

    private void requireSupportedMediaType(String bizType, String mediaType) {
        if (!BIZ_MEDIA_TYPES.get(bizType).contains(mediaType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前业务不支持该媒体类型");
        }
    }

    private String normalizeAssetId(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return null;
        }
        String normalizedAssetId = assetId.trim();
        if (!normalizedAssetId.matches("\\d+")) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体资产 ID 格式不正确");
        }
        return normalizedAssetId;
    }

    private String resolveFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
