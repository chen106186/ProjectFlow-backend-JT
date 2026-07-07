package com.jitong.projectflow.file.service;

import com.jitong.projectflow.file.dto.FileResponse;

import java.io.InputStream;

public record FileDownloadResult(FileResponse metadata, InputStream inputStream) {
}
