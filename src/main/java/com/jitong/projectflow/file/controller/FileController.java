package com.jitong.projectflow.file.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.file.dto.FileBatchDownloadRequest;
import com.jitong.projectflow.file.dto.FileBatchDeleteRequest;
import com.jitong.projectflow.file.dto.FileFolderCreateRequest;
import com.jitong.projectflow.file.dto.FileFolderResponse;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.service.FileDownloadResult;
import com.jitong.projectflow.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "附件管理", description = "业务附件与文档中心文件的上传、查询、下载和删除接口。")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "上传附件或文档中心文件",
            description = "上传指定业务类型和业务ID下的文件，支持版本号、存储位置和文件分类，便于文档中心按类别检索。")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> upload(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(required = false) String versionNo,
            @RequestParam(required = false) String storageLocation,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) Long folderId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(
                fileService.upload(businessType, businessId, versionNo, storageLocation, fileCategory, folderId, file),
                MDC.get("traceId")
        );
    }

    @Operation(summary = "查询业务附件列表",
            description = "按业务类型、业务ID查询已上传文件，支持通过文件分类、关键词和文件夹筛选。")
    @GetMapping
    public ApiResponse<List<FileResponse>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long folderId) {
        return ApiResponse.success(fileService.list(businessType, businessId, fileCategory, keyword, folderId), MDC.get("traceId"));
    }

    @Operation(summary = "下载附件",
            description = "根据附件ID下载原始文件，并返回正确的文件名和内容类型。")
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        FileDownloadResult result = fileService.download(id);
        FileResponse metadata = result.metadata();
        String encodedName = URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = StringUtils.hasText(metadata.getContentType())
                ? MediaType.parseMediaType(metadata.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(metadata.getFileSize() == null ? 0 : metadata.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(summary = "删除附件",
            description = "删除指定附件，并校验业务数据访问权限。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "批量删除附件",
            description = "按文件ID列表批量删除附件，逐条校验业务数据访问权限。")
    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteBatch(@Valid @RequestBody FileBatchDeleteRequest request) {
        fileService.deleteBatch(request.getIds());
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "新建文件夹", description = "在指定业务类型和业务ID下新建文件夹，用于文档中心组织文件。")
    @PostMapping("/folders")
    public ApiResponse<FileFolderResponse> createFolder(@Valid @RequestBody FileFolderCreateRequest request) {
        return ApiResponse.success(fileService.createFolder(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询文件夹列表", description = "按业务类型和业务ID查询已创建的文件夹。")
    @GetMapping("/folders")
    public ApiResponse<List<FileFolderResponse>> listFolders(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return ApiResponse.success(fileService.listFolders(businessType, businessId), MDC.get("traceId"));
    }

    @Operation(summary = "富文本图片上传",
            description = "上传富文本编辑器内嵌图片，返回 wangEditor 所需的 JSON 格式 {errno,data:{url}}。")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> uploadImage(@RequestPart("file") MultipartFile file) {
        try {
            FileResponse resp = fileService.upload("RICH_TEXT", 0L, null, null, null, file);
            String url = "/api/files/" + resp.getId() + "/inline";
            return Map.of("errno", 0, "data", Map.of("url", url, "alt", "", "href", ""));
        } catch (Exception e) {
            return Map.of("errno", 1, "message", e.getMessage() != null ? e.getMessage() : "上传失败");
        }
    }

    @Operation(summary = "内联预览文件",
            description = "以内联方式返回文件内容，供富文本嵌入图片使用，无需鉴权。")
    @GetMapping("/{id}/inline")
    public ResponseEntity<InputStreamResource> inline(@PathVariable Long id) {
        FileDownloadResult result = fileService.download(id);
        FileResponse metadata = result.metadata();
        String encodedName = URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = StringUtils.hasText(metadata.getContentType())
                ? MediaType.parseMediaType(metadata.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(metadata.getFileSize() == null ? 0 : metadata.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(summary = "通过存储Key预览富文本图片",
            description = "兼容历史富文本中保存的私有 OSS 直链，仅允许已登录用户预览数据库中登记的富文本图片。")
    @GetMapping("/rich-text-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> inlineRichTextImage(@RequestParam String key) {
        FileDownloadResult result = fileService.downloadRichTextImageByStorageKey(key);
        FileResponse metadata = result.metadata();
        String encodedName = URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = StringUtils.hasText(metadata.getContentType())
                ? MediaType.parseMediaType(metadata.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(metadata.getFileSize() == null ? 0 : metadata.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(summary = "批量下载文件", description = "按文件ID列表批量打包下载，以 ZIP 格式返回。")
    @PostMapping("/batch-download")
    public ResponseEntity<InputStreamResource> batchDownload(@RequestBody FileBatchDownloadRequest request) {
        java.io.PipedInputStream pipedIn = new java.io.PipedInputStream();
        try {
            java.io.PipedOutputStream pipedOut = new java.io.PipedOutputStream(pipedIn);
            new Thread(() -> {
                try {
                    fileService.batchDownload(request.getFileIds(), request.getFolderIds(), pipedOut);
                } catch (Exception ignored) {
                } finally {
                    try { pipedOut.close(); } catch (java.io.IOException ignored2) {}
                }
            }).start();
        } catch (java.io.IOException e) {
            throw new com.jitong.projectflow.common.error.BusinessException(
                    com.jitong.projectflow.common.error.ErrorCode.BAD_REQUEST, "批量下载失败");
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.zip\"")
                .body(new InputStreamResource(pipedIn));
    }
}
