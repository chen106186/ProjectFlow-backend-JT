package com.jitong.projectflow.file.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    private Long id;
    private String businessType;
    private Long businessId;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String versionNo;
    private String storageType;
    private Long uploaderId;
    private LocalDateTime uploadedAt;
}
