package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Tag(name = "4-8. 관리자 이미지 파일 드래그앤드랍 업로드 API", description = "관리자 콘솔에서 메뉴 이미지를 드래그앤드랍으로 업로드하여 고유 이미지 URL을 발급받습니다.")
@RestController
@RequestMapping("/api/v1/admin/images")
@SecurityRequirement(name = "bearerAuth")
public class AdminImageUploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String SERVER_IMAGE_DIR = "/var/www/desserting-images/menus";
    private static final String LOCAL_IMAGE_DIR = "uploads/menus";
    private static final String PUBLIC_URL_PREFIX = "https://desserting-api.duckdns.org/images/menus/";

    @Operation(summary = "메뉴 이미지 파일 업로드 (드래그앤드랍, ADM-IMG)", description = "JPG, PNG, WEBP 등의 이미지 파일을 업로드하고 서빙 URL을 반환합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 이미지 확장자입니다: " + ext);
        }

        // 고유 파일명 생성
        String uniqueFilename = "menu_" + UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis() + "." + ext;

        // 저장 디렉토리 결정
        Path targetDir = Paths.get(SERVER_IMAGE_DIR);
        boolean isServerDir = true;
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
        } catch (Exception e) {
            log.warn("서버 이미지 디렉토리(/var/www/desserting-images/menus) 접근 실패, 로컬 경로로 전환: {}", e.getMessage());
            targetDir = Paths.get(LOCAL_IMAGE_DIR);
            isServerDir = false;
            try {
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
            } catch (IOException ex) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 저장 디렉토리 생성 실패");
            }
        }

        Path targetPath = targetDir.resolve(uniqueFilename);
        try {
            file.transferTo(targetPath.toFile());
            log.info("메뉴 이미지 업로드 완료: filename={}, path={}, size={} bytes", uniqueFilename, targetPath, file.getSize());
        } catch (IOException e) {
            log.error("이미지 파일 저장 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }

        String fullUrl = isServerDir
                ? PUBLIC_URL_PREFIX + uniqueFilename
                : "/images/menus/" + uniqueFilename;

        return ResponseEntity.ok(ApiResponse.success("이미지가 성공적으로 업로드되었습니다.", Map.of(
                "imageUrl", fullUrl,
                "filename", uniqueFilename,
                "size", String.valueOf(file.getSize())
        )));
    }
}
