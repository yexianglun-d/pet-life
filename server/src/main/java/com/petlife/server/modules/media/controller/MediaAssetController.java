package com.petlife.server.modules.media.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import com.petlife.server.modules.media.service.MediaAssetApplicationService;
import com.petlife.server.modules.media.service.MediaAssetContent;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体资产接口。
 */
@RestController
@RequestMapping("/api/v1/media-assets")
public class MediaAssetController {

    private final MediaAssetApplicationService mediaAssetApplicationService;

    public MediaAssetController(MediaAssetApplicationService mediaAssetApplicationService) {
        this.mediaAssetApplicationService = mediaAssetApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaAssetResponse> uploadMediaAsset(
        @RequestParam("biz_type") String bizType,
        @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(mediaAssetApplicationService.uploadMediaAsset(bizType, file));
    }

    @GetMapping("/{assetId}")
    public ApiResponse<MediaAssetResponse> getMediaAsset(@PathVariable("assetId") Long assetId) {
        return ApiResponse.success(mediaAssetApplicationService.getMediaAsset(assetId));
    }

    @GetMapping("/{assetId}/content")
    public ResponseEntity<Resource> getMediaAssetContent(@PathVariable("assetId") Long assetId) {
        MediaAssetContent mediaAssetContent = mediaAssetApplicationService.getMediaAssetContent(assetId);
        MediaType contentType = MediaType.parseMediaType(mediaAssetContent.mediaAsset().getContentType());
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(mediaAssetContent.mediaAsset().getFileName())
                .build()
                .toString())
            .body(mediaAssetContent.resource());
    }
}
