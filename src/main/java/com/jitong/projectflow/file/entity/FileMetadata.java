package com.jitong.projectflow.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pf_file")
public class FileMetadata {
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableLogic
    private Integer deleted;
}
