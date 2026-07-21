package com.jitong.projectflow.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@EnableConfigurationProperties(AliyunOssStorageProperties.class)
@ConditionalOnProperty(prefix = "projectflow.storage", name = "type", havingValue = "aliyun-oss")
public class AliyunOssFileStorageService implements FileStorageService {
    private final OSS ossClient;
    private final AliyunOssStorageProperties properties;

    public AliyunOssFileStorageService(AliyunOssStorageProperties properties) {
        validateProperties(properties);
        this.properties = properties;
        try {
            this.ossClient = new OSSClientBuilder().build(
                    properties.endpoint(),
                    properties.accessKeyId(),
                    properties.accessKeySecret());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Aliyun OSS client init failed. endpoint=" + properties.endpoint()
                    + ", bucket=" + properties.bucketName() + ". Cause: " + e.getMessage(), e);
        }
    }

    private static void validateProperties(AliyunOssStorageProperties p) {
        if (p.endpoint() == null || p.endpoint().isBlank())
            throw new IllegalStateException("projectflow.storage.aliyun-oss.endpoint is required");
        if (p.accessKeyId() == null || p.accessKeyId().isBlank())
            throw new IllegalStateException("projectflow.storage.aliyun-oss.access-key-id is required");
        if (p.accessKeySecret() == null || p.accessKeySecret().isBlank())
            throw new IllegalStateException("projectflow.storage.aliyun-oss.access-key-secret is required");
        if (p.bucketName() == null || p.bucketName().isBlank())
            throw new IllegalStateException("projectflow.storage.aliyun-oss.bucket-name is required");
    }

    @Override
    public StoredFile upload(FileUploadCommand command) {
        String objectKey = buildObjectKey(command.originalName());
        ossClient.putObject(properties.bucketName(), objectKey, command.inputStream());
        return new StoredFile("ALIYUN_OSS", objectKey, command.fileSize());
    }

    @Override
    public InputStream download(String storageKey) {
        return ossClient.getObject(properties.bucketName(), normalizeKey(storageKey)).getObjectContent();
    }

    @Override
    public void delete(String storageKey) {
        ossClient.deleteObject(properties.bucketName(), normalizeKey(storageKey));
    }

    @Override
    public String publicUrl(String storageKey) {
        String endpointHost = properties.endpoint().replaceFirst("https?://", "");
        return "https://" + properties.bucketName() + "." + endpointHost + "/" + normalizeKey(storageKey);
    }

    @PreDestroy
    public void shutdown() {
        ossClient.shutdown();
    }

    private String buildObjectKey(String originalName) {
        String raw = properties.objectPrefix();
        String prefix = (raw == null || raw.isBlank()) ? "projectflow" : raw;
        String safeName = originalName.replaceAll("[\\\\/]", "_");
        return prefix + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
    }

    // OSS keys must not start with '/', but an empty objectPrefix misconfiguration can produce them.
    // Strip leading slashes so legacy DB records with broken keys still resolve correctly.
    private static String normalizeKey(String key) {
        int i = 0;
        while (i < key.length() && key.charAt(i) == '/') i++;
        return i == 0 ? key : key.substring(i);
    }
}
