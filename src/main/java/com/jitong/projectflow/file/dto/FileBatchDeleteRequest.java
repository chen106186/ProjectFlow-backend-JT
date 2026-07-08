package com.jitong.projectflow.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "批量删除文件请求。")
public class FileBatchDeleteRequest {
    @NotEmpty(message = "文件ID列表不能为空")
    @Schema(description = "需要删除的文件ID列表。", example = "[1,2,3]")
    private List<Long> ids = new ArrayList<>();
}
