package com.petlife.server.modules.media.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.petlife.server.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageServiceTests {

    @TempDir
    private Path tempDirectory;

    @Test
    void shouldStoreLocalMediaObjectAndLoadResource() throws Exception {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setRootPath(tempDirectory.toString());
        properties.setBucketName("petlife-local-test");
        MediaStorageService mediaStorageService = new MediaStorageService(properties);

        StoredMediaObject storedMediaObject = mediaStorageService.store(
            10001L,
            "daily_log",
            "momo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            new MockMultipartFile(
                "file",
                "momo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "hello-petlife".getBytes(StandardCharsets.UTF_8)
            )
        );

        assertEquals("petlife-local-test", storedMediaObject.bucketName());
        assertTrue(storedMediaObject.objectKey().startsWith("daily_log/"));
        assertEquals(MediaType.IMAGE_JPEG_VALUE, storedMediaObject.contentType());
        assertEquals(13L, storedMediaObject.fileSize());
        assertNotNull(storedMediaObject.fileHash());
        assertTrue(mediaStorageService.loadAsResource(storedMediaObject.objectKey()).isReadable());
    }

    @Test
    void shouldReserveObjectStorageMetadataWithCdnAccessUrl() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setProvider("object_storage");
        properties.setRootPath(tempDirectory.toString());
        properties.setBucketName("petlife-oss-test");
        properties.setPublicBaseUrl("https://cdn.petlife.example/media/");
        MediaStorageService mediaStorageService = new MediaStorageService(properties);

        StoredMediaObject storedMediaObject = mediaStorageService.store(
            10001L,
            "health_report",
            "report.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            new MockMultipartFile(
                "file",
                "report.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf-content".getBytes(StandardCharsets.UTF_8)
            )
        );

        assertTrue(mediaStorageService.isObjectStorageProvider());
        assertEquals("petlife-oss-test", storedMediaObject.bucketName());
        assertTrue(storedMediaObject.objectKey().startsWith("health_report/"));
        assertEquals("https://cdn.petlife.example/media/" + storedMediaObject.objectKey(), storedMediaObject.accessUrl());
    }

    @Test
    void shouldRejectUnsupportedStorageProvider() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setProvider("unsupported");
        MediaStorageService mediaStorageService = new MediaStorageService(properties);

        assertThrows(
            BusinessException.class,
            () -> mediaStorageService.store(
                10001L,
                "daily_log",
                "momo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new MockMultipartFile(
                    "file",
                    "momo.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "hello".getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }
}
