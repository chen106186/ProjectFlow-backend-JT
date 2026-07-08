package com.jitong.projectflow.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "业务类型，例如 PROJECT、TASK、BUG、REQUIREMENT。")
    private String businessType;
    @Schema(description = "业务数据 ID。")
    private Long businessId;
    @Schema(description = "上传时的原始文件名。")
    private String originalName;
    @Schema(description = "文件 MIME 类型。")
    private String contentType;
    @Schema(description = "文件大小，单位字节。")
    private Long fileSize;
    @Schema(description = "附件版本号。")
    private String versionNo;
    @Schema(description = "存储类型，当前支持 LOCAL，后续预留 MINIO、OSS。")
    private String storageType;
    @Schema(description = "上传人 ID。")
    private Long uploaderId;
    @Schema(description = "上传时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime uploadedAt;
}
