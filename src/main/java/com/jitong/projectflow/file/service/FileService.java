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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileService {
    private static final String DEFAULT_VERSION = "v1";
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Pattern NUMERIC_VERSION = Pattern.compile("^v(\\d+)$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("docx", "xlsx", "pdf", "png", "jpg", "jpeg", "drawio");

    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;

    public FileResponse upload(String businessType, Long businessId, String versionNo, MultipartFile file) {
        validateUpload(businessType, businessId, file);
        String originalName = file.getOriginalFilename();
        String resolvedVersionNo = StringUtils.hasText(versionNo) ? versionNo : nextVersionNo(businessType, businessId, originalName);
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
            metadata.setVersionNo(resolvedVersionNo);
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
        if (!StringUtils.hasText(businessType) || businessId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File query requires businessType and businessId");
        }
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getBusinessType, businessType);
        wrapper.eq(FileMetadata::getBusinessId, businessId);
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
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File name is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File size exceeds 50 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File type not allowed");
        }
    }

    private String nextVersionNo(String businessType, Long businessId, String originalName) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getBusinessType, businessType);
        wrapper.eq(FileMetadata::getBusinessId, businessId);
        wrapper.eq(FileMetadata::getOriginalName, originalName);
        int maxVersion = fileMetadataMapper.selectList(wrapper).stream()
                .map(FileMetadata::getVersionNo)
                .map(this::numericVersion)
                .reduce(0, Math::max);
        return maxVersion == 0 ? DEFAULT_VERSION : "v" + (maxVersion + 1);
    }

    private int numericVersion(String versionNo) {
        if (!StringUtils.hasText(versionNo)) {
            return 0;
        }
        Matcher matcher = NUMERIC_VERSION.matcher(versionNo);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
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
