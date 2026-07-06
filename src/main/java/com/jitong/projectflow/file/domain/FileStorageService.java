package com.jitong.projectflow.file.domain;

import java.io.InputStream;

public interface FileStorageService {
    StoredFile upload(FileUploadCommand command);
    InputStream download(String storageKey);
    void delete(String storageKey);
}
