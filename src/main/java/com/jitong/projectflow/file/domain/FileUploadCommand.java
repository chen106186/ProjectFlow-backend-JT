package com.jitong.projectflow.file.domain;

import java.io.InputStream;

public record FileUploadCommand(
        String originalName,
        String contentType,
        long fileSize,
        InputStream inputStream
) {
}
