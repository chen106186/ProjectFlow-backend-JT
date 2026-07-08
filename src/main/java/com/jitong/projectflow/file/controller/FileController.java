package com.jitong.projectflow.file.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.service.FileDownloadResult;
import com.jitong.projectflow.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "附件管理", description = "业务附件上传、查询、下载和删除接口")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "上传附件",
            description = "上传指定业务类型和业务 ID 下的附件，支持版本号记录。")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<FileResponse> upload(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(required = false) String versionNo,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(businessType, businessId, versionNo, file), MDC.get("traceId"));
    }

    @Operation(summary = "查询业务附件列表",
            description = "按业务类型和业务 ID 查询已上传附件列表。")
    @GetMapping
    public ApiResponse<List<FileResponse>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return ApiResponse.success(fileService.list(businessType, businessId), MDC.get("traceId"));
    }

    @Operation(summary = "下载附件",
            description = "根据附件 ID 下载原始文件，并返回正确的文件名和内容类型。")
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
            description = "逻辑删除指定附件，并校验业务数据访问权限。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
