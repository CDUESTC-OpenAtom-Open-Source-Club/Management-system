package com.openatom.club.file.service;

import com.openatom.club.common.config.StorageProperties;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.repository.FileRecordRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final StorageProperties storageProperties;
    private final FileRecordRepository fileRecordRepository;

    public FileRecord saveFile(MultipartFile file, String moduleName, String subDir,
                                Set<String> allowedExtensions) throws IOException {
        String originalName = file.getOriginalFilename();
        validateExtension(originalName, allowedExtensions);

        String ext = getExtension(originalName);
        String storedName = UUID.randomUUID() + "." + ext;

        Path dir = Path.of(storageProperties.getRoot(), subDir);
        Files.createDirectories(dir);
        Path filePath = dir.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        FileRecord record = new FileRecord();
        record.setOriginalName(originalName);
        record.setStoredName(storedName);
        record.setFilePath(filePath.toString());
        record.setFileSize(file.getSize());
        record.setContentType(file.getContentType());
        record.setModuleName(moduleName);
        record.setUploadedBy(ActorHolder.get().getName());
        return fileRecordRepository.save(record);
    }

    public void downloadFile(Long fileId, HttpServletResponse response) throws IOException {
        serveFile(fileId, response, false);
    }

    /** 在线查看文件（Content-Disposition: inline） */
    public void viewFile(Long fileId, HttpServletResponse response) throws IOException {
        serveFile(fileId, response, true);
    }

    private void serveFile(Long fileId, HttpServletResponse response, boolean inline) throws IOException {
        FileRecord record = fileRecordRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> BizException.of("文件不存在"));
        Path path = Path.of(record.getFilePath());
        if (!Files.exists(path)) throw BizException.of("文件已丢失");

        String contentType = record.getContentType() != null ? record.getContentType() : "application/octet-stream";
        response.setContentType(contentType);
        String disposition = inline ? "inline" : "attachment";
        response.setHeader("Content-Disposition",
                disposition + "; filename=\"" + encodeFileName(record.getOriginalName()) + "\"");
        response.setContentLengthLong(record.getFileSize() != null ? record.getFileSize() : Files.size(path));
        try (InputStream is = Files.newInputStream(path);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    public FileRecord getFileRecord(Long fileId) {
        return fileRecordRepository.findByIdAndDeletedAtIsNull(fileId).orElse(null);
    }

    @Transactional
    public void softDeleteFileRecord(Long fileId) {
        fileRecordRepository.findByIdAndDeletedAtIsNull(fileId).ifPresent(record -> {
            record.setDeletedAt(OffsetDateTime.now());
            fileRecordRepository.save(record);
        });
    }

    public void validateExtension(String fileName, Set<String> allowed) {
        String ext = getExtension(fileName).toLowerCase();
        if (!allowed.contains(ext)) {
            throw BizException.of("不支持的文件类型: " + ext + "，允许: " + allowed);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String encodeFileName(String name) {
        try {
            return java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            return name;
        }
    }
}
