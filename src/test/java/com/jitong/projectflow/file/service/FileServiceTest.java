package com.jitong.projectflow.file.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.auth.security.BusinessAccessService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    FileMetadataMapper fileMetadataMapper;
    @Mock
    FileStorageService fileStorageService;
    @Mock
    BusinessAccessService businessAccessService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void uploadDelegatesToStorageAndPersistsMetadata() {
        CurrentUserContext.set(1001L);
        when(fileStorageService.upload(any())).thenReturn(new StoredFile("LOCAL", "2026-07-07/a.pdf", 3));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

        new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, "v1", file);

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileStorageService).upload(any());
        verify(fileMetadataMapper).insert(captor.capture());
        FileMetadata metadata = captor.getValue();
        assertThat(metadata.getBusinessType()).isEqualTo("TASK");
        assertThat(metadata.getBusinessId()).isEqualTo(10L);
        assertThat(metadata.getOriginalName()).isEqualTo("a.pdf");
        assertThat(metadata.getStorageType()).isEqualTo("LOCAL");
        assertThat(metadata.getStorageKey()).isEqualTo("2026-07-07/a.pdf");
        assertThat(metadata.getUploaderId()).isEqualTo(1001L);
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", "abc".getBytes());

        assertThatThrownBy(() -> new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, null, file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的文件类型");
    }

    @Test
    void uploadRejectsFilesOverMaxSize() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]) {
            @Override
            public long getSize() {
                return 50L * 1024 * 1024 + 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };

        assertThatThrownBy(() -> new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, null, file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件大小不能超过50MB");
    }

    @Test
    void uploadRejectsBlankOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "", "application/pdf", "abc".getBytes());

        assertThatThrownBy(() -> new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, null, file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件名不能为空");
    }

    @Test
    void uploadGeneratesFirstVersionWhenVersionBlank() {
        when(fileMetadataMapper.selectList(any())).thenReturn(List.of());
        when(fileStorageService.upload(any())).thenReturn(new StoredFile("LOCAL", "2026-07-07/a.pdf", 3));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

        new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, null, file);

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo("v1");
    }

    @Test
    void uploadGeneratesNextNumericVersion() {
        FileMetadata v1 = new FileMetadata();
        v1.setVersionNo("v1");
        FileMetadata v3 = new FileMetadata();
        v3.setVersionNo("v3");
        FileMetadata draft = new FileMetadata();
        draft.setVersionNo("draft");
        when(fileMetadataMapper.selectList(any())).thenReturn(List.of(v1, v3, draft));
        when(fileStorageService.upload(any())).thenReturn(new StoredFile("LOCAL", "2026-07-07/a.pdf", 3));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

        new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, "", file);

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo("v4");
    }

    @Test
    void uploadKeepsExplicitVersion() {
        when(fileStorageService.upload(any())).thenReturn(new StoredFile("LOCAL", "2026-07-07/a.pdf", 3));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

        new FileService(fileMetadataMapper, fileStorageService, businessAccessService).upload("TASK", 10L, "review", file);

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo("review");
    }

    @Test
    void listRejectsMissingBusinessType() {
        assertThatThrownBy(() -> new FileService(fileMetadataMapper, fileStorageService, businessAccessService).list(null, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("查询文件需要指定业务类型和业务ID");
    }

    @Test
    void listRejectsMissingBusinessId() {
        assertThatThrownBy(() -> new FileService(fileMetadataMapper, fileStorageService, businessAccessService).list("TASK", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("查询文件需要指定业务类型和业务ID");
    }
}
