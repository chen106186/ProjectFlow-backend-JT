package com.jitong.projectflow.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import com.jitong.projectflow.file.dto.FileFolderCreateRequest;
import com.jitong.projectflow.file.dto.FileFolderResponse;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.entity.FileFolderEntity;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.file.mapper.FileFolderMapper;
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
    private static final String DEFAULT_STORAGE_LOCATION = "BUSINESS";
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Pattern NUMERIC_VERSION = Pattern.compile("^v(\\d+)$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("docx", "xlsx", "pdf", "png", "jpg", "jpeg", "drawio");

    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;
    private final BusinessAccessService businessAccessService;
    private final FileFolderMapper fileFolderMapper;

    public FileResponse upload(String businessType, Long businessId, String versionNo, MultipartFile file) {
        return upload(businessType, businessId, versionNo, DEFAULT_STORAGE_LOCATION, null, file);
    }

    public FileResponse upload(String businessType, Long businessId, String versionNo,
                               String storageLocation, String fileCategory, MultipartFile file) {
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
            metadata.setStorageLocation(StringUtils.hasText(storageLocation) ? storageLocation : DEFAULT_STORAGE_LOCATION);
            metadata.setFileCategory(StringUtils.hasText(fileCategory) ? fileCategory : null);
            metadata.setStorageType(storedFile.storageType());
            metadata.setStorageKey(storedFile.storageKey());
            metadata.setUploaderId(CurrentUserContext.userIdOrNull());
            metadata.setUploadedAt(LocalDateTime.now());
            fileMetadataMapper.insert(metadata);
            return toResponse(metadata);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败");
        }
    }

    public List<FileResponse> list(String businessType, Long businessId) {
        return list(businessType, businessId, null, null, null);
    }

    public List<FileResponse> list(String businessType, Long businessId, String fileCategory) {
        return list(businessType, businessId, fileCategory, null, null);
    }

    public List<FileResponse> list(String businessType, Long businessId, String fileCategory, String keyword, Long folderId) {
        if (!StringUtils.hasText(businessType) || businessId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "查询文件需要指定业务类型和业务ID");
        }
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getBusinessType, businessType);
        wrapper.eq(FileMetadata::getBusinessId, businessId);
        wrapper.eq(StringUtils.hasText(fileCategory), FileMetadata::getFileCategory, fileCategory);
        wrapper.like(org.springframework.util.StringUtils.hasText(keyword), FileMetadata::getOriginalName, keyword);
        wrapper.eq(folderId != null, FileMetadata::getFolderId, folderId);
        wrapper.orderByDesc(FileMetadata::getUploadedAt);
        return fileMetadataMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public FileDownloadResult download(Long id) {
        FileMetadata metadata = requireFile(id);
        return new FileDownloadResult(toResponse(metadata), fileStorageService.download(metadata.getStorageKey()));
    }

    public void delete(Long id) {
        FileMetadata metadata = requireFile(id);
        businessAccessService.requireFileDelete(metadata);
        fileMetadataMapper.deleteById(id);
        fileStorageService.delete(metadata.getStorageKey());
    }

    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件ID列表不能为空");
        }
        ids.forEach(this::delete);
    }

    private void validateUpload(String businessType, Long businessId, MultipartFile file) {
        if (!StringUtils.hasText(businessType) || businessId == null || file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的文件上传请求");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过50MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return metadata;
    }

    public FileFolderResponse createFolder(FileFolderCreateRequest request) {
        FileFolderEntity entity = new FileFolderEntity();
        entity.setBusinessType(request.getBusinessType());
        entity.setBusinessId(request.getBusinessId());
        entity.setName(request.getName());
        entity.setCreatedBy(com.jitong.projectflow.auth.security.CurrentUserContext.userIdOrNull());
        fileFolderMapper.insert(entity);
        return toFolderResponse(entity);
    }

    public List<FileFolderResponse> listFolders(String businessType, Long businessId) {
        return fileFolderMapper.selectList(
                new LambdaQueryWrapper<FileFolderEntity>()
                        .eq(org.springframework.util.StringUtils.hasText(businessType), FileFolderEntity::getBusinessType, businessType)
                        .eq(businessId != null, FileFolderEntity::getBusinessId, businessId)
                        .orderByAsc(FileFolderEntity::getCreatedAt))
                .stream().map(this::toFolderResponse).toList();
    }

    public void batchDownload(List<Long> ids, java.io.OutputStream out) {
        List<FileMetadata> files = fileMetadataMapper.selectBatchIds(ids);
        if (files.isEmpty()) {
            throw new com.jitong.projectflow.common.error.BusinessException(
                    com.jitong.projectflow.common.error.ErrorCode.BAD_REQUEST, "未找到可下载的文件");
        }
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            for (FileMetadata meta : files) {
                FileDownloadResult result = download(meta.getId());
                String entryName = meta.getOriginalName() != null ? meta.getOriginalName() : meta.getId().toString();
                zip.putNextEntry(new java.util.zip.ZipEntry(entryName));
                result.inputStream().transferTo(zip);
                zip.closeEntry();
            }
        } catch (java.io.IOException e) {
            throw new com.jitong.projectflow.common.error.BusinessException(
                    com.jitong.projectflow.common.error.ErrorCode.BAD_REQUEST, "批量下载失败：" + e.getMessage());
        }
    }

    private FileFolderResponse toFolderResponse(FileFolderEntity entity) {
        return FileFolderResponse.builder()
                .id(entity.getId())
                .businessType(entity.getBusinessType())
                .businessId(entity.getBusinessId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
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
                .storageLocation(metadata.getStorageLocation())
                .fileCategory(metadata.getFileCategory())
                .storageType(metadata.getStorageType())
                .url(fileStorageService.publicUrl(metadata.getStorageKey()))
                .uploaderId(metadata.getUploaderId())
                .uploadedAt(metadata.getUploadedAt())
                .build();
    }
}
