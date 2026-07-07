package com.jitong.projectflow.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private static final String DEFAULT_VERSION = "v1";
    private static final String DEFAULT_FILE_NAME = "file";

    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;

    public FileResponse upload(String businessType, Long businessId, String versionNo, MultipartFile file) {
        validateUpload(businessType, businessId, file);
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : DEFAULT_FILE_NAME;
        try {
            StoredFile storedFile = fileStorageService.upload(new FileUploadCommand(
                    originalName,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream()
            ));
            FileMetadata metadata = new FileMetadata();
            metadata.setBusinessType(businessType);
            metadata.setBusinessId(businessId);
            metadata.setOriginalName(originalName);
            metadata.setContentType(file.getContentType());
            metadata.setFileSize(storedFile.fileSize());
            metadata.setVersionNo(StringUtils.hasText(versionNo) ? versionNo : DEFAULT_VERSION);
            metadata.setStorageType(storedFile.storageType());
            metadata.setStorageKey(storedFile.storageKey());
            metadata.setUploaderId(CurrentUserContext.userIdOrNull());
            metadata.setUploadedAt(LocalDateTime.now());
            fileMetadataMapper.insert(metadata);
            return toResponse(metadata);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "File upload failed");
        }
    }

    public List<FileResponse> list(String businessType, Long businessId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(businessType), FileMetadata::getBusinessType, businessType);
        wrapper.eq(businessId != null, FileMetadata::getBusinessId, businessId);
        wrapper.orderByDesc(FileMetadata::getUploadedAt);
        return fileMetadataMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public FileDownloadResult download(Long id) {
        FileMetadata metadata = requireFile(id);
        return new FileDownloadResult(toResponse(metadata), fileStorageService.download(metadata.getStorageKey()));
    }

    public void delete(Long id) {
        FileMetadata metadata = requireFile(id);
        fileMetadataMapper.deleteById(id);
        fileStorageService.delete(metadata.getStorageKey());
    }

    private void validateUpload(String businessType, Long businessId, MultipartFile file) {
        if (!StringUtils.hasText(businessType) || businessId == null || file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file upload request");
        }
    }

    private FileMetadata requireFile(Long id) {
        FileMetadata metadata = fileMetadataMapper.selectById(id);
        if (metadata == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "File not found");
        }
        return metadata;
    }

    private FileResponse toResponse(FileMetadata metadata) {
        return FileResponse.builder()
                .id(metadata.getId())
                .businessType(metadata.getBusinessType())
                .businessId(metadata.getBusinessId())
                .originalName(metadata.getOriginalName())
                .contentType(metadata.getContentType())
                .fileSize(metadata.getFileSize())
                .versionNo(metadata.getVersionNo())
                .storageType(metadata.getStorageType())
                .uploaderId(metadata.getUploaderId())
                .uploadedAt(metadata.getUploadedAt())
                .build();
    }
}
