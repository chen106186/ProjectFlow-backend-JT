package com.jitong.projectflow.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "文件夹响应。")
public class FileFolderResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "业务类型。")
    private String businessType;
    @Schema(description = "业务 ID。")
    private Long businessId;
    @Schema(description = "文件夹名称。")
    private String name;
    @Schema(description = "创建时间。")
    private LocalDateTime createdAt;
}
