package com.familog.server.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.config.FamilogProperties;
import com.familog.server.exception.BusinessException;
import com.familog.server.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 공유 데이터 디렉토리({familog.data-dir}) 파일 저장 담당.
 * DB에는 "/files/..." 상대 URL만 저장한다 (05 아키텍처 §6).
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FamilogProperties properties;

    /** MultipartFile을 데이터 디렉토리 하위 상대경로에 저장하고 그 상대경로를 반환 */
    public String store(MultipartFile file, String relativePath) {
        try {
            Path target = resolve(relativePath);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return relativePath;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    public void copy(String sourceRelativePath, String targetRelativePath) {
        try {
            Path target = resolve(targetRelativePath);
            Files.createDirectories(target.getParent());
            Files.copy(resolve(sourceRelativePath), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    public Path resolve(String relativePath) {
        return Paths.get(properties.dataDir()).resolve(relativePath);
    }

    /** 상대경로 → 프론트가 로드할 URL 경로 */
    public String toFileUrl(String relativePath) {
        return "/files/" + relativePath.replace("\\", "/");
    }

    public String extensionOf(MultipartFile file, String fallback) {
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            return fallback;
        }
        return original.substring(original.lastIndexOf('.') + 1).toLowerCase();
    }
}
