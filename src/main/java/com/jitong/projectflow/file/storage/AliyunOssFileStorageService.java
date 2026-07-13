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
        this.properties = properties;
        this.ossClient = new OSSClientBuilder().build(
                properties.endpoint(),
                properties.accessKeyId(),
                properties.accessKeySecret());
    }

    @Override
    public StoredFile upload(FileUploadCommand command) {
        String objectKey = buildObjectKey(command.originalName());
        ossClient.putObject(properties.bucketName(), objectKey, command.inputStream());
        return new StoredFile("ALIYUN_OSS", objectKey, command.fileSize());
    }

    @Override
    public InputStream download(String storageKey) {
        return ossClient.getObject(properties.bucketName(), storageKey).getObjectContent();
    }

    @Override
    public void delete(String storageKey) {
        ossClient.deleteObject(properties.bucketName(), storageKey);
    }

    @Override
    public String publicUrl(String storageKey) {
        String endpointHost = properties.endpoint().replaceFirst("https?://", "");
        return "https://" + properties.bucketName() + "." + endpointHost + "/" + storageKey;
    }

    @PreDestroy
    public void shutdown() {
        ossClient.shutdown();
    }

    private String buildObjectKey(String originalName) {
        String prefix = properties.objectPrefix() == null ? "projectflow" : properties.objectPrefix();
        String safeName = originalName.replaceAll("[\\\\/]", "_");
        return prefix + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
    }
}
