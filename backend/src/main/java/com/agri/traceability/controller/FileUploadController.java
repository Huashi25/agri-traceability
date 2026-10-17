package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-url}")
    private String accessUrl;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return Result.error("仅支持 JPG/PNG/GIF/WEBP 格式");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        File destDir = new File(uploadPath + "/" + dateDir);
        if (!destDir.exists() && !destDir.mkdirs()) {
            return Result.error("创建目录失败");
        }

        File destFile = new File(destDir, fileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            return Result.error("文件保存失败: " + e.getMessage());
        }

        String url = accessUrl + "/" + dateDir + "/" + fileName;
        return Result.success(url);
    }
}
