package com.jitong.projectflow.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pf_file")
public class FileMetadata {
    private Long id;
    private String businessType;
    private Long businessId;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String versionNo;
    private String storageType;
    private String storageKey;
    private Long uploaderId;
    private LocalDateTime uploadedAt;
    private Boolean deleted;
}
