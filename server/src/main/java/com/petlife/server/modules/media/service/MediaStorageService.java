package com.petlife.server.modules.media.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体存储服务。
 */
@Service
public class MediaStorageService {

    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String PROVIDER_LOCAL = "local";
    private static final String PROVIDER_OBJECT_STORAGE = "object_storage";
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of(PROVIDER_LOCAL, PROVIDER_OBJECT_STORAGE);

    private final MediaStorageProperties properties;

    public MediaStorageService(MediaStorageProperties properties) {
        this.properties = properties;
    }

    public StoredMediaObject store(
        Long uploaderUserId,
        String bizType,
        String normalizedFileName,
        String contentType,
        MultipartFile file
    ) {
        String provider = normalizeProvider();
        String objectKey = buildObjectKey(uploaderUserId, bizType, normalizedFileName);
        Path targetPath = resolveObjectPath(objectKey);
        try {
            /*
             * object_storage 当前是迁移预留模式：先统一对象 key、bucket 和 CDN URL 口径，
             * 文件仍写入本地过渡目录，后续替换为云厂商上传适配器时不影响业务表与 asset_id。
             */
            Files.createDirectories(targetPath.getParent());
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
                Files.copy(digestInputStream, targetPath);
            }
            long fileSize = Files.size(targetPath);
            String fileHash = HEX_FORMAT.formatHex(messageDigest.digest());
            return new StoredMediaObject(
                objectKey,
                normalizeBucketName(provider),
                buildAccessUrl(objectKey),
                contentType,
                fileSize,
                fileHash
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteQuietly(targetPath);
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "媒体文件保存失败");
        }
    }

    public Resource loadAsResource(String objectKey) {
        Path objectPath = resolveObjectPath(objectKey);
        try {
            Resource resource = new UrlResource(objectPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ResponseCode.MEDIA_ASSET_NOT_FOUND, "媒体文件不存在");
            }
            return resource;
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.MEDIA_ASSET_NOT_FOUND, "媒体文件不存在");
        }
    }

    public long getMaxFileSizeBytes() {
        return properties.getMaxFileSizeBytes();
    }

    public boolean isObjectStorageProvider() {
        return PROVIDER_OBJECT_STORAGE.equals(normalizeProvider());
    }

    private String normalizeProvider() {
        String provider = normalizeNullableText(properties.getProvider());
        if (provider == null) {
            return PROVIDER_LOCAL;
        }
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体存储类型仅支持 local 或 object_storage");
        }
        return provider;
    }

    private String normalizeBucketName(String provider) {
        String bucketName = normalizeNullableText(properties.getBucketName());
        if (bucketName != null) {
            return bucketName;
        }
        return PROVIDER_OBJECT_STORAGE.equals(provider) ? "petlife-object-storage" : "petlife-local";
    }

    private String buildObjectKey(Long uploaderUserId, String bizType, String normalizedFileName) {
        LocalDate today = LocalDate.now();
        String extension = resolveExtension(normalizedFileName);
        return "%s/%04d/%02d/%s/%s%s".formatted(
            bizType,
            today.getYear(),
            today.getMonthValue(),
            uploaderUserId,
            UUID.randomUUID(),
            extension
        );
    }

    private String resolveExtension(String normalizedFileName) {
        int dotIndex = normalizedFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedFileName.length() - 1) {
            return "";
        }
        String extension = normalizedFileName.substring(dotIndex).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,12}") ? extension : "";
    }

    private String buildAccessUrl(String objectKey) {
        String publicBaseUrl = normalizeNullableText(properties.getPublicBaseUrl());
        if (publicBaseUrl == null) {
            return null;
        }
        String normalizedBaseUrl = publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
        return normalizedBaseUrl + "/" + objectKey;
    }

    private Path resolveObjectPath(String objectKey) {
        Path rootPath = Path.of(properties.getRootPath()).toAbsolutePath().normalize();
        Path objectPath = rootPath.resolve(objectKey).normalize();
        if (!objectPath.startsWith(rootPath)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "媒体对象路径不合法");
        }
        return objectPath;
    }

    private void deleteQuietly(Path targetPath) {
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // 保存失败后的清理不影响原始异常返回。
        }
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        return normalizedText.isEmpty() ? null : normalizedText;
    }
}
