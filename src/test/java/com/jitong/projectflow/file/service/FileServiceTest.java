package com.jitong.projectflow.file.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.StoredFile;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    FileMetadataMapper fileMetadataMapper;
    @Mock
    FileStorageService fileStorageService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void uploadDelegatesToStorageAndPersistsMetadata() {
        CurrentUserContext.set(1001L);
        when(fileStorageService.upload(any())).thenReturn(new StoredFile("LOCAL", "2026-07-07/a.txt", 3));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "abc".getBytes());

        new FileService(fileMetadataMapper, fileStorageService).upload("TASK", 10L, "v1", file);

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileStorageService).upload(any());
        verify(fileMetadataMapper).insert(captor.capture());
        FileMetadata metadata = captor.getValue();
        assertThat(metadata.getBusinessType()).isEqualTo("TASK");
        assertThat(metadata.getBusinessId()).isEqualTo(10L);
        assertThat(metadata.getOriginalName()).isEqualTo("a.txt");
        assertThat(metadata.getStorageType()).isEqualTo("LOCAL");
        assertThat(metadata.getStorageKey()).isEqualTo("2026-07-07/a.txt");
        assertThat(metadata.getUploaderId()).isEqualTo(1001L);
    }
}
