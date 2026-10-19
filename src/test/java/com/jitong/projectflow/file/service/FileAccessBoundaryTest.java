package com.jitong.projectflow.file.service;

import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.file.mapper.FileFolderMapper;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.report.mapper.ProjectReportMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileAccessBoundaryTest {
    private final FileMetadataMapper files = mock(FileMetadataMapper.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final TaskMapper tasks = mock(TaskMapper.class);
    private FileService service;

    @BeforeEach
    void createRealAuthorizationBoundary() {
        ProjectMapper projects = mock(ProjectMapper.class);
        BusinessAccessService access = new BusinessAccessService(projects);
        FileBusinessAccessService business = new FileBusinessAccessService(access, projects, tasks,
                mock(BugMapper.class), mock(RequirementMapper.class), mock(DailyReportMapper.class), mock(ProjectReportMapper.class));
        service = new FileService(files, storage, access, mock(FileFolderMapper.class), mock(SystemUserMapper.class), business);
    }

    @AfterEach
    void clearIdentity() {
        CurrentUserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousInlineCannotReadProjectAttachmentEvenWhenItIsAnImage() {
        when(files.selectById(1L)).thenReturn(file(1L, "PROJECT", 10L, "image/png"));
        assertThatThrownBy(() -> service.inline(1L)).isInstanceOf(BusinessException.class).hasMessage("未登录或登录已过期");
        verify(storage, never()).download(any());
    }

    @Test
    void anonymousInlineCannotReadActiveContentMasqueradingAsAnEditorImage() {
        when(files.selectById(1L)).thenReturn(file(1L, "RICH_TEXT", 0L, "image/svg+xml"));
        assertThatThrownBy(() -> service.inline(1L)).isInstanceOf(BusinessException.class);
        verify(storage, never()).download(any());
    }

    @Test
    void anonymousEditorRasterImageRemainsDisplayable() throws Exception {
        when(files.selectById(1L)).thenReturn(file(1L, "rich-text", 0L, "image/png"));
        when(storage.download("object-1")).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        try (var input = service.inline(1L).inputStream()) {
            assertThat(input.readAllBytes()).containsExactly(1, 2, 3);
        }
    }

    @Test
    void legacyEditorImageLinkRejectsSvgEvenForSignedInUser() {
        CurrentUserContext.set(100L);
        FileMetadata image = file(1L, "RICH_TEXT", 0L, "image/svg+xml");
        image.setStorageKey("projectflow/image.svg");
        when(files.selectOne(any())).thenReturn(image);
        assertThatThrownBy(() -> service.downloadRichTextImageByStorageKey("projectflow/image.svg"))
                .isInstanceOf(BusinessException.class).hasMessage("仅支持预览图片");
        verify(storage, never()).download(any());
    }

    @Test
    void authenticatedTaskAssigneeCanDownloadHistoricalTypeAlias() {
        CurrentUserContext.set(100L);
        when(files.selectById(1L)).thenReturn(file(1L, "Task", 10L, "application/pdf"));
        TaskEntity task = new TaskEntity();
        task.setId(10L);
        task.setAssigneeId(100L);
        when(tasks.selectById(10L)).thenReturn(task);
        service.download(1L);
        verify(storage).download("object-1");
    }

    @Test
    void unrelatedUserCannotDownloadOrPreviewTaskAttachment() {
        CurrentUserContext.set(200L);
        when(files.selectById(1L)).thenReturn(file(1L, "TASK", 10L, "application/pdf"));
        TaskEntity task = new TaskEntity();
        task.setAssigneeId(100L);
        when(tasks.selectById(10L)).thenReturn(task);
        assertThatThrownBy(() -> service.download(1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.inline(1L)).isInstanceOf(BusinessException.class);
        verify(storage, never()).download(any());
    }

    @Test
    void zipChecksEveryFileBeforeWritingAnyBytes() {
        CurrentUserContext.set(100L);
        FileMetadata own = file(1L, "TASK", 10L, "application/pdf");
        FileMetadata other = file(2L, "TASK", 20L, "application/pdf");
        when(files.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(own, other));
        TaskEntity ownTask = new TaskEntity();
        ownTask.setAssigneeId(100L);
        TaskEntity otherTask = new TaskEntity();
        otherTask.setAssigneeId(200L);
        when(tasks.selectById(10L)).thenReturn(ownTask);
        when(tasks.selectById(20L)).thenReturn(otherTask);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThatThrownBy(() -> service.batchDownload(List.of(1L, 2L), output)).isInstanceOf(BusinessException.class);
        assertThat(output.size()).isZero();
        verify(storage, never()).download(any());
    }

    private FileMetadata file(Long id, String type, Long businessId, String contentType) {
        FileMetadata file = new FileMetadata();
        file.setId(id);
        file.setBusinessType(type);
        file.setBusinessId(businessId);
        file.setContentType(contentType);
        file.setStorageKey("object-" + id);
        file.setOriginalName("attachment");
        return file;
    }
}
