package com.jitong.projectflow.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileFolderCreateRequest {
    @NotNull
    @Schema(description = "业务类型，例如 PROJECT。")
    private String businessType;
    @NotNull
    @Schema(description = "业务ID。")
    private Long businessId;
    @NotBlank
    @Schema(description = "文件夹名称。")
    private String name;
}
