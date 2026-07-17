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
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
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
    private final SystemUserMapper systemUserMapper;

    public FileResponse upload(String businessType, Long businessId, String versionNo, MultipartFile file) {
        return upload(businessType, businessId, versionNo, DEFAULT_STORAGE_LOCATION, null, file);
    }

    public FileResponse upload(String businessType, Long businessId, String versionNo,
                               String storageLocation, String fileCategory, MultipartFile file) {
        return upload(businessType, businessId, versionNo, storageLocation, fileCategory, null, file);
    }

    public FileResponse upload(String businessType, Long businessId, String versionNo,
                               String storageLocation, String fileCategory, Long folderId, MultipartFile file) {
        validateUpload(businessType, businessId, file);
        validateFolder(businessType, businessId, folderId);
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
            metadata.setFolderId(folderId);
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

    public FileDownloadResult downloadRichTextImageByStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !storageKey.startsWith("projectflow/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不合法");
        }
        FileMetadata metadata = fileMetadataMapper.selectOne(new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getBusinessType, "RICH_TEXT")
                .eq(FileMetadata::getStorageKey, storageKey)
                .last("LIMIT 1"));
        if (metadata == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        if (!StringUtils.hasText(metadata.getContentType()) || !metadata.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持预览图片");
        }
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

    public void deleteFolder(Long id) {
        FileFolderEntity folder = fileFolderMapper.selectById(id);
        if (folder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件夹不存在");
        }
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getFolderId, id);
        List<FileMetadata> filesInFolder = fileMetadataMapper.selectList(wrapper);
        filesInFolder.forEach(file -> {
            file.setFolderId(null);
            fileMetadataMapper.updateById(file);
        });
        fileFolderMapper.deleteById(id);
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
        batchDownload(ids, null, out);
    }

    public void batchDownload(List<Long> fileIds, List<Long> folderIds, java.io.OutputStream out) {
        List<Long> selectedFileIds = fileIds == null ? List.of() : fileIds;
        List<Long> selectedFolderIds = folderIds == null ? List.of() : folderIds;
        List<FileMetadata> rootFiles = selectedFileIds.isEmpty() ? List.of() : fileMetadataMapper.selectBatchIds(selectedFileIds);
        List<FileFolderEntity> selectedFolders = selectedFolderIds.isEmpty() ? List.of() : fileFolderMapper.selectBatchIds(selectedFolderIds);
        java.util.Set<Long> folderIdsForPath = new java.util.LinkedHashSet<>(selectedFolderIds);
        rootFiles.stream().map(FileMetadata::getFolderId).filter(java.util.Objects::nonNull).forEach(folderIdsForPath::add);
        java.util.Map<Long, FileFolderEntity> folderMap = folderIdsForPath.isEmpty()
                ? java.util.Map.of()
                : fileFolderMapper.selectBatchIds(folderIdsForPath).stream()
                .collect(java.util.stream.Collectors.toMap(FileFolderEntity::getId, folder -> folder, (left, right) -> left, java.util.LinkedHashMap::new));
        List<FileMetadata> folderFiles = selectedFolderIds.isEmpty()
                ? List.of()
                : fileMetadataMapper.selectList(new LambdaQueryWrapper<FileMetadata>().in(FileMetadata::getFolderId, selectedFolderIds));
        if (rootFiles.isEmpty() && selectedFolders.isEmpty()) {
            throw new com.jitong.projectflow.common.error.BusinessException(
                    com.jitong.projectflow.common.error.ErrorCode.BAD_REQUEST, "未找到可下载的文件");
        }
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            java.util.Set<String> usedEntryNames = new java.util.HashSet<>();
            java.util.Map<Long, String> folderEntryNames = new java.util.HashMap<>();
            for (FileFolderEntity folder : selectedFolders) {
                String folderName = uniqueEntryName(usedEntryNames, sanitizeZipName(folder.getName()) + "/");
                folderEntryNames.put(folder.getId(), folderName);
                zip.putNextEntry(new java.util.zip.ZipEntry(folderName));
                zip.closeEntry();
                for (FileMetadata meta : folderFiles.stream().filter(file -> folder.getId().equals(file.getFolderId())).toList()) {
                    writeZipFile(zip, usedEntryNames, meta, folderName);
                }
            }
            for (FileMetadata meta : rootFiles) {
                if (meta.getFolderId() != null && folderMap.containsKey(meta.getFolderId())) {
                    if (selectedFolderIds.contains(meta.getFolderId())) {
                        continue;
                    }
                    String folderName = folderEntryNames.get(meta.getFolderId());
                    if (folderName == null) {
                        FileFolderEntity folder = folderMap.get(meta.getFolderId());
                        folderName = uniqueEntryName(usedEntryNames, sanitizeZipName(folder.getName()) + "/");
                        folderEntryNames.put(folder.getId(), folderName);
                        zip.putNextEntry(new java.util.zip.ZipEntry(folderName));
                        zip.closeEntry();
                    }
                    writeZipFile(zip, usedEntryNames, meta, folderName);
                    continue;
                }
                writeZipFile(zip, usedEntryNames, meta, "");
            }
        } catch (java.io.IOException e) {
            throw new com.jitong.projectflow.common.error.BusinessException(
                    com.jitong.projectflow.common.error.ErrorCode.BAD_REQUEST, "批量下载失败：" + e.getMessage());
        }
    }

    private void writeZipFile(java.util.zip.ZipOutputStream zip, java.util.Set<String> usedEntryNames,
                              FileMetadata meta, String prefix) throws java.io.IOException {
        FileDownloadResult result = download(meta.getId());
        String fileName = meta.getOriginalName() != null ? meta.getOriginalName() : meta.getId().toString();
        String entryName = uniqueEntryName(usedEntryNames, prefix + sanitizeZipName(fileName));
        zip.putNextEntry(new java.util.zip.ZipEntry(entryName));
        result.inputStream().transferTo(zip);
        zip.closeEntry();
    }

    private String sanitizeZipName(String name) {
        if (!StringUtils.hasText(name)) {
            return "未命名";
        }
        return name.replace("\\", "_").replace("/", "_").replace(":", "_").replace("*", "_")
                .replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
    }

    private String uniqueEntryName(java.util.Set<String> usedEntryNames, String entryName) {
        if (usedEntryNames.add(entryName)) {
            return entryName;
        }
        boolean directory = entryName.endsWith("/");
        String normalized = directory ? entryName.substring(0, entryName.length() - 1) : entryName;
        int dot = normalized.lastIndexOf('.');
        String base = dot > 0 ? normalized.substring(0, dot) : normalized;
        String ext = dot > 0 ? normalized.substring(dot) : "";
        int index = 2;
        String candidate;
        do {
            candidate = base + "(" + index++ + ")" + ext + (directory ? "/" : "");
        } while (!usedEntryNames.add(candidate));
        return candidate;
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
                .folderId(metadata.getFolderId())
                .storageType(metadata.getStorageType())
                .url(fileStorageService.publicUrl(metadata.getStorageKey()))
                .uploaderId(metadata.getUploaderId())
                .uploaderName(resolveUploaderName(metadata.getUploaderId()))
                .uploadedAt(metadata.getUploadedAt())
                .build();
    }

    private String resolveUploaderName(Long uploaderId) {
        if (uploaderId == null) {
            return null;
        }
        SystemUser user = systemUserMapper.selectById(uploaderId);
        return user == null ? null : user.getRealName();
    }

    private void validateFolder(String businessType, Long businessId, Long folderId) {
        if (folderId == null) {
            return;
        }
        FileFolderEntity folder = fileFolderMapper.selectById(folderId);
        if (folder == null
                || !businessType.equals(folder.getBusinessType())
                || !businessId.equals(folder.getBusinessId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件夹不存在或不属于当前业务");
        }
    }
}
