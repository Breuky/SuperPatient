package com.superpatient.service;

import com.superpatient.dto.UploadVO;
import com.superpatient.entity.UploadFile;
import com.superpatient.mapper.UploadFileMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final UploadFileMapper uploadFileMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 保存图片并登记归属，返回的 url 指向受鉴权保护的下载接口。
     */
    public UploadVO upload(MultipartFile file, String type) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("仅支持 jpg/png 格式的图片");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String newFileName = "report_" + dateStr + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        Path filePath = dir.resolve(newFileName).normalize();
        if (!filePath.startsWith(dir)) {
            throw new RuntimeException("非法的文件名");
        }
        file.transferTo(filePath.toFile());

        UploadFile record = new UploadFile();
        record.setUserId(CurrentHolder.getCurrentId());
        record.setFileName(newFileName);
        record.setOriginalName(originalFilename);
        record.setContentType(resolveContentType(extension));
        record.setSizeBytes(file.getSize());
        record.setBizType(type == null ? "report" : type);
        uploadFileMapper.insert(record);

        return new UploadVO(fileUrl(record.getId()), newFileName, file.getSize());
    }

    /**
     * 读取图片内容。非本人上传的文件一律返回 null（对外表现为 404，不泄露存在性）。
     */
    public FileContent load(Long id) throws IOException {
        UploadFile record = uploadFileMapper.findById(id);
        if (record == null || !Objects.equals(record.getUserId(), CurrentHolder.getCurrentId())) {
            return null;
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = dir.resolve(record.getFileName()).normalize();
        if (!filePath.startsWith(dir) || !Files.isRegularFile(filePath)) {
            return null;
        }
        return new FileContent(new FileSystemResource(filePath), record.getContentType());
    }

    /**
     * 图片访问地址。前端展示时需带上 token（浏览器 img 标签无法自定义请求头）。
     */
    public static String fileUrl(Long id) {
        return "/api/files/" + id;
    }

    private String resolveContentType(String extension) {
        return "png".equals(extension) ? "image/png" : "image/jpeg";
    }

    public record FileContent(Resource resource, String contentType) {
    }
}
