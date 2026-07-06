package com.jitong.projectflow.file.storage;

import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "projectflow.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    private final Path root;

    public LocalFileStorageService(@Value("${projectflow.storage.local-root}") String localRoot) {
        this.root = Path.of(localRoot);
    }

    @Override
    public StoredFile upload(FileUploadCommand command) {
        try {
            String datePath = LocalDate.now().toString();
            String safeName = UUID.randomUUID() + "-" + command.originalName().replaceAll("[\\\\/]", "_");
            Path target = root.resolve(datePath).resolve(safeName).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(command.inputStream(), target);
            return new StoredFile("LOCAL", root.relativize(target).toString().replace("\\", "/"), command.fileSize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件上传失败", ex);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return Files.newInputStream(root.resolve(storageKey).normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件下载失败", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(root.resolve(storageKey).normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件删除失败", ex);
        }
    }
}
