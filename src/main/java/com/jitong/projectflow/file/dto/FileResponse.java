package com.jitong.projectflow.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "文件元数据响应。")
public class FileResponse {
    @Schema(description = "主键ID。")
    private Long id;
    @Schema(description = "业务类型，例如 PROJECT、TASK、BUG、REQUIREMENT。")
    private String businessType;
    @Schema(description = "业务数据ID。")
    private Long businessId;
    @Schema(description = "上传时的原始文件名。")
    private String originalName;
    @Schema(description = "文件MIME类型。")
    private String contentType;
    @Schema(description = "文件大小，单位字节。")
    private Long fileSize;
    @Schema(description = "附件版本号。")
    private String versionNo;
    @Schema(description = "存储位置，用于区分业务附件、文档中心等场景。")
    private String storageLocation;
    @Schema(description = "文件分类，例如 DESIGN、TEST、DELIVERY、MEETING。")
    private String fileCategory;
    @Schema(description = "所属文件夹 ID，根目录文件为空。")
    private Long folderId;
    @Schema(description = "底层存储类型，当前支持 LOCAL，并预留 MINIO、OSS。")
    private String storageType;
    @Schema(description = "OSS 文件公开访问 URL，LOCAL 存储时为 null。")
    private String url;
    @Schema(description = "上传人ID。")
    private Long uploaderId;
    @Schema(description = "上传时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime uploadedAt;
}
