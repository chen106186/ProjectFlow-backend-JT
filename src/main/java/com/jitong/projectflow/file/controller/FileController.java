package com.jitong.projectflow.file.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.service.FileDownloadResult;
import com.jitong.projectflow.file.service.FileService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<FileResponse> upload(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(required = false) String versionNo,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(businessType, businessId, versionNo, file), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<List<FileResponse>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return ApiResponse.success(fileService.list(businessType, businessId), MDC.get("traceId"));
    }

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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
