package com.jitong.projectflow.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pf_file_folder")
public class FileFolderEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String businessType;
    private Long businessId;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
