package com.jitong.projectflow.file.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileBatchDownloadRequest {
    private List<Long> fileIds;
    private List<Long> folderIds;
}
