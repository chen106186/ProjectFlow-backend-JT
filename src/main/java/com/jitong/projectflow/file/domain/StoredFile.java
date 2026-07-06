package com.jitong.projectflow.file.domain;

public record StoredFile(String storageType, String storageKey, long fileSize) {
}
