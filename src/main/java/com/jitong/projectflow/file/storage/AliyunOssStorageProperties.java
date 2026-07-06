package com.jitong.projectflow.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectflow.storage.aliyun-oss")
public record AliyunOssStorageProperties(
        String endpoint,
        String accessKeyId,
        String accessKeySecret,
        String bucketName,
        String objectPrefix
) {
}
