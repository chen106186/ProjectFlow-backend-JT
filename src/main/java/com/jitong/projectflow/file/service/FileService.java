package com.jitong.projectflow.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileBusinessType;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileService {
    public static final String ITERATION_EVIDENCE_CATEGORY = "ITERATION_EVIDENCE";
    private static final String DEFAULT_VERSION = "v1";
    private static final String DEFAULT_STORAGE_LOCATION = "BUSINESS";
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final long MAX_SCREEN_RECORDING_SIZE = 100L * 1024 * 1024;
    private static final Pattern NUMERIC_VERSION = Pattern.compile("^v(\\d+)$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("docx", "xlsx", "pptx", "pdf", "png", "jpg", "jpeg", "drawio", "md", "txt", "csv");
    private static final Map<String, String> SCREEN_RECORDING_TYPES = Map.of(
            "mp4", "video/mp4", "webm", "video/webm", "mov", "video/quicktime");
    private static final Set<String> PUBLIC_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/avif", "image/bmp", "image/heic", "image/heif");

    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;
    private final BusinessAccessService businessAccessService;
    private final FileFolderMapper fileFolderMapper;
    private final SystemUserMapper systemUserMapper;
    private final FileBusinessAccessService fileBusinessAccessService;

    public FileResponse upload(String businessType, Long businessId, String versionNo, MultipartFile file) {
        return upload(businessType, businessId, versionNo, DEFAULT_STORAGE_LOCATION, null, file);
    }

    public FileResponse uploadIterationEvidence(Long projectId, MultipartFile file) {
        return upload("PROJECT", projectId, null, "RELEASE_ITERATION", ITERATION_EVIDENCE_CATEGORY, null, file);
    }

    public FileResponse upload(String businessType, Long businessId, String versionNo,
                               String storageLocation, String fileCategory, MultipartFile file) {
        return upload(businessType, businessId, versionNo, storageLocation, fileCategory, null, file);
    }

    public FileResponse upload(String businessType, Long businessId, String versionNo,
                               String storageLocation, String fileCategory, Long folderId, MultipartFile file) {
        validateUpload(businessType, businessId, fileCategory, file);
        businessType = FileBusinessType.from(businessType).name();
        fileBusinessAccessService.requireRead(businessType, businessId);
        validateFolder(businessType, businessId, folderId);
        String originalName = file.getOriginalFilename();
        String contentType = SCREEN_RECORDING_TYPES.getOrDefault(extensionOf(originalName), file.getContentType());
        String resolvedVersionNo = StringUtils.hasText(versionNo) ? versionNo : nextVersionNo(businessType, businessId, originalName);
        try {
            StoredFile storedFile = fileStorageService.upload(new FileUploadCommand(
                    originalName,
                    contentType,
                    file.getSize(),
                    file.getInputStream()
            ));
            FileMetadata metadata = new FileMetadata();
            metadata.setBusinessType(businessType);
            metadata.setBusinessId(businessId);
            metadata.setOriginalName(originalName);
            metadata.setContentType(contentType);
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
        fileBusinessAccessService.requireRead(businessType, businessId);
        validateFolder(businessType, businessId, folderId);
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileMetadata::getBusinessType, FileBusinessType.from(businessType).aliases());
        wrapper.eq(FileMetadata::getBusinessId, businessId);
        if (FileBusinessType.from(businessType) == FileBusinessType.RICH_TEXT
                && !businessAccessService.isSystemAdmin()) {
            wrapper.eq(FileMetadata::getUploaderId, CurrentUserContext.userId());
        }
        wrapper.eq(StringUtils.hasText(fileCategory), FileMetadata::getFileCategory, fileCategory);
        wrapper.like(org.springframework.util.StringUtils.hasText(keyword), FileMetadata::getOriginalName, keyword);
        wrapper.eq(folderId != null, FileMetadata::getFolderId, folderId);
        wrapper.orderByDesc(FileMetadata::getUploadedAt);
        return fileMetadataMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public FileDownloadResult download(Long id) {
        FileMetadata metadata = requireFile(id);
        fileBusinessAccessService.requireRead(metadata.getBusinessType(), metadata.getBusinessId());
        return new FileDownloadResult(toResponse(metadata), fileStorageService.download(metadata.getStorageKey()));
    }

    public FileDownloadResult inline(Long id) {
        FileMetadata metadata = requireFile(id);
        boolean publicEditorImage = FileBusinessType.from(metadata.getBusinessType()) == FileBusinessType.RICH_TEXT
                && metadata.getContentType() != null
                && PUBLIC_IMAGE_TYPES.contains(metadata.getContentType().toLowerCase(Locale.ROOT));
        if (!publicEditorImage) {
            fileBusinessAccessService.requireRead(metadata.getBusinessType(), metadata.getBusinessId());
        }
        return new FileDownloadResult(toResponse(metadata), fileStorageService.download(metadata.getStorageKey()));
    }

    public FileDownloadResult downloadRichTextImageByStorageKey(String storageKey) {
        CurrentUserContext.userId();
        if (!StringUtils.hasText(storageKey) || !storageKey.startsWith("projectflow/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不合法");
        }
        FileMetadata metadata = fileMetadataMapper.selectOne(new LambdaQueryWrapper<FileMetadata>()
                .in(FileMetadata::getBusinessType, FileBusinessType.RICH_TEXT.aliases())
                .eq(FileMetadata::getStorageKey, storageKey)
                .last("LIMIT 1"));
        if (metadata == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        if (metadata.getContentType() == null
                || !PUBLIC_IMAGE_TYPES.contains(metadata.getContentType().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持预览图片");
        }
        return new FileDownloadResult(toResponse(metadata), fileStorageService.download(metadata.getStorageKey()));
    }

    @Transactional
    public void delete(Long id) {
        FileMetadata metadata = requireFile(id);
        requireReadableOrDeletedOwner(metadata);
        businessAccessService.requireFileDelete(metadata);
        requireNotIterationEvidence(metadata);
        deleteMetadataAndMaybeObject(metadata);
    }

    /** Called after the owning service has authorized deletion, within its transaction. */
    @Transactional
    public void purgeForBusiness(String businessType, Long businessId) {
        List<String> aliases = FileBusinessType.from(businessType).aliases();
        List<FileMetadata> files = fileMetadataMapper.selectList(new LambdaQueryWrapper<FileMetadata>()
                .in(FileMetadata::getBusinessType, aliases).eq(FileMetadata::getBusinessId, businessId));
        files.forEach(this::requireNotIterationEvidence);
        files.forEach(this::deleteMetadataAndMaybeObject);
        fileFolderMapper.selectList(new LambdaQueryWrapper<FileFolderEntity>()
                .in(FileFolderEntity::getBusinessType, aliases).eq(FileFolderEntity::getBusinessId, businessId))
                .forEach(folder -> fileFolderMapper.deleteById(folder.getId()));
    }

    private void requireReadableOrDeletedOwner(FileMetadata metadata) {
        try {
            fileBusinessAccessService.requireRead(metadata.getBusinessType(), metadata.getBusinessId());
        } catch (BusinessException exception) {
            if (exception.errorCode() != ErrorCode.NOT_FOUND) throw exception;
            // Historical deletes may have left attachment rows behind. Their uploader or an admin may remove them.
        }
    }

    private void deleteMetadataAndMaybeObject(FileMetadata metadata) {
        // A locking read sees references committed while this transaction was waiting, unlike a snapshot count.
        List<FileMetadata> references = fileMetadataMapper.selectList(new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getStorageKey, metadata.getStorageKey())
                .orderByAsc(FileMetadata::getId).last("FOR UPDATE"));
        if (fileMetadataMapper.deleteById(metadata.getId()) == 0) return;
        if (references.stream().noneMatch(file -> !metadata.getId().equals(file.getId()))) {
            Runnable removeObject = () -> fileStorageService.delete(metadata.getStorageKey());
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { removeObject.run(); }
                });
            } else {
                removeObject.run();
            }
        }
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件ID列表不能为空");
        }
        ids.forEach(id -> {
            FileMetadata metadata = requireFile(id);
            requireReadableOrDeletedOwner(metadata);
            businessAccessService.requireFileDelete(metadata);
            requireNotIterationEvidence(metadata);
        });
        ids.stream().distinct().forEach(this::delete);
    }

    private void requireNotIterationEvidence(FileMetadata metadata) {
        if (ITERATION_EVIDENCE_CATEGORY.equals(metadata.getFileCategory())) {
            throw new BusinessException(ErrorCode.CONFLICT, "迭代档案凭证需永久留存，不能删除");
        }
    }

    private void validateUpload(String businessType, Long businessId, String fileCategory, MultipartFile file) {
        if (!StringUtils.hasText(businessType) || businessId == null || file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的文件上传请求");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String videoContentType = SCREEN_RECORDING_TYPES.get(extension);
        if ("SCREEN_RECORDING".equals(fileCategory) && videoContentType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "录屏仅支持 MP4、WebM、MOV 格式");
        }
        if (videoContentType != null) {
            if (FileBusinessType.from(businessType) != FileBusinessType.BUG
                    || !"SCREEN_RECORDING".equals(fileCategory)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "录屏仅支持作为 Bug 附件上传");
            }
            String submittedType = file.getContentType();
            if (submittedType != null && !submittedType.isBlank()
                    && !videoContentType.equalsIgnoreCase(submittedType)
                    && !"application/octet-stream".equalsIgnoreCase(submittedType)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "录屏文件类型与扩展名不匹配");
            }
            if (file.getSize() > MAX_SCREEN_RECORDING_SIZE) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "录屏大小不能超过100MB");
            }
            return;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过50MB");
        }
        // 所有 image/* 类型（包括 HEIC、WebP、GIF 等）直接放行
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return;
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }
    }

    private String nextVersionNo(String businessType, Long businessId, String originalName) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileMetadata::getBusinessType, FileBusinessType.from(businessType).aliases());
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

    @Transactional
    public void deleteFolder(Long id) {
        FileFolderEntity folder = fileFolderMapper.selectById(id);
        if (folder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件夹不存在");
        }
        fileBusinessAccessService.requireRead(folder.getBusinessType(), folder.getBusinessId());
        if (!businessAccessService.isSystemAdmin() && !CurrentUserContext.userId().equals(folder.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据");
        }
        fileMetadataMapper.update(null, new LambdaUpdateWrapper<FileMetadata>()
                .eq(FileMetadata::getFolderId, id).set(FileMetadata::getFolderId, null));
        fileFolderMapper.deleteById(id);
    }

    public FileFolderResponse createFolder(FileFolderCreateRequest request) {
        fileBusinessAccessService.requireRead(request.getBusinessType(), request.getBusinessId());
        FileFolderEntity entity = new FileFolderEntity();
        entity.setBusinessType(FileBusinessType.from(request.getBusinessType()).name());
        entity.setBusinessId(request.getBusinessId());
        entity.setName(request.getName());
        entity.setCreatedBy(com.jitong.projectflow.auth.security.CurrentUserContext.userIdOrNull());
        fileFolderMapper.insert(entity);
        return toFolderResponse(entity);
    }

    public List<FileFolderResponse> listFolders(String businessType, Long businessId) {
        fileBusinessAccessService.requireRead(businessType, businessId);
        LambdaQueryWrapper<FileFolderEntity> wrapper = new LambdaQueryWrapper<FileFolderEntity>()
                        .in(FileFolderEntity::getBusinessType, FileBusinessType.from(businessType).aliases())
                        .eq(FileFolderEntity::getBusinessId, businessId)
                        .orderByAsc(FileFolderEntity::getCreatedAt);
        if (FileBusinessType.from(businessType) == FileBusinessType.RICH_TEXT
                && !businessAccessService.isSystemAdmin()) {
            wrapper.eq(FileFolderEntity::getCreatedBy, CurrentUserContext.userId());
        }
        return fileFolderMapper.selectList(wrapper)
                .stream().map(this::toFolderResponse).toList();
    }

    public void batchDownload(List<Long> ids, java.io.OutputStream out) {
        batchDownload(ids, null, out);
    }

    /** Validate the full selection on the request thread, before the controller commits a ZIP response. */
    public void requireBatchDownloadAccess(List<Long> fileIds, List<Long> folderIds) {
        List<Long> selectedFileIds = fileIds == null ? List.of() : fileIds;
        List<Long> selectedFolderIds = folderIds == null ? List.of() : folderIds;
        List<FileMetadata> files = selectedFileIds.isEmpty() ? List.of() : fileMetadataMapper.selectBatchIds(selectedFileIds);
        List<FileFolderEntity> folders = selectedFolderIds.isEmpty() ? List.of() : fileFolderMapper.selectBatchIds(selectedFolderIds);
        if (files.size() != selectedFileIds.stream().distinct().count()
                || folders.size() != selectedFolderIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件或文件夹不存在");
        }
        files.forEach(file -> fileBusinessAccessService.requireRead(file.getBusinessType(), file.getBusinessId()));
        folders.forEach(folder -> fileBusinessAccessService.requireRead(folder.getBusinessType(), folder.getBusinessId()));
        if (!selectedFolderIds.isEmpty()) {
            fileMetadataMapper.selectList(new LambdaQueryWrapper<FileMetadata>()
                    .in(FileMetadata::getFolderId, selectedFolderIds))
                    .forEach(file -> fileBusinessAccessService.requireRead(file.getBusinessType(), file.getBusinessId()));
        }
        if (files.isEmpty() && folders.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到可下载的文件");
        }
    }

    public void batchDownload(List<Long> fileIds, List<Long> folderIds, java.io.OutputStream out) {
        requireBatchDownloadAccess(fileIds, folderIds);
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
        // Check every selected object before emitting even the first byte of a ZIP response.
        rootFiles.forEach(file -> fileBusinessAccessService.requireRead(file.getBusinessType(), file.getBusinessId()));
        selectedFolders.forEach(folder -> fileBusinessAccessService.requireRead(folder.getBusinessType(), folder.getBusinessId()));
        folderFiles.forEach(file -> fileBusinessAccessService.requireRead(file.getBusinessType(), file.getBusinessId()));
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
        try (var input = result.inputStream()) {
            input.transferTo(zip);
        }
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
                || FileBusinessType.from(businessType) != FileBusinessType.from(folder.getBusinessType())
                || !businessId.equals(folder.getBusinessId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件夹不存在或不属于当前业务");
        }
    }
}
