package hxy.dragon.service.impl;

import hxy.dragon.config.AiFileProperties;
import hxy.dragon.dao.mapper.FileMapper;
import hxy.dragon.dao.model.FileModel;
import hxy.dragon.service.AiFileService;
import hxy.dragon.service.FileService;
import hxy.dragon.util.DateUtil;
import hxy.dragon.util.DiskUtil;
import hxy.dragon.util.Streams;
import hxy.dragon.util.file.DirUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 与 {@link hxy.dragon.service.impl.FileServiceImpl} 一致：multipart 由 Jakarta 解析（spring.servlet.multipart.enabled=false）。
 */
@Slf4j
@Service
public class AiFileServiceImpl implements AiFileService {

    @Autowired
    private AiFileProperties aiFileProperties;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileService fileService;

    private String sanitizePath(String path) {
        if (path == null) {
            return "";
        }
        String sanitized = path.replaceAll("\\\\", "/");
        sanitized = sanitized.replaceAll("\\.\\./", "");
        sanitized = sanitized.replaceAll("\\.\\.\\\\", "");
        sanitized = sanitized.replaceAll("/\\./", "/");
        sanitized = sanitized.replaceAll("\\.\\.", "");
        sanitized = sanitized.replaceAll("^[/\\\\]+", "");
        return sanitized;
    }

    private String ensurePathWithinRoot(String basePath, String relativePath) {
        try {
            Path normalizedBase = Paths.get(basePath).normalize().toAbsolutePath();
            Path normalizedPath = Paths.get(basePath, sanitizePath(relativePath)).normalize().toAbsolutePath();
            if (!normalizedPath.startsWith(normalizedBase)) {
                log.error("Attempted directory traversal: {} outside {}", normalizedPath, normalizedBase);
                return basePath;
            }
            return normalizedPath.toString();
        } catch (Exception e) {
            log.error("Path validation error", e);
            return basePath;
        }
    }

    private String buildDownloadUrl(HttpServletRequest request, String fileUuid) {
        String base = aiFileProperties.getPublicBaseUrl();
        if (base == null) {
            base = "";
        }
        base = base.trim();
        if (base.isEmpty()) {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (!StringUtils.hasText(scheme)) {
                scheme = request.getScheme();
            }
            String host = request.getHeader("X-Forwarded-Host");
            if (!StringUtils.hasText(host)) {
                host = request.getHeader("Host");
            }
            if (!StringUtils.hasText(host)) {
                host = request.getServerName();
                int port = request.getServerPort();
                if (("http".equalsIgnoreCase(scheme) && port != 80)
                        || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                    host = host + ":" + port;
                }
            }
            base = scheme + "://" + host;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        return base + ctx + "/api/ai-files/" + fileUuid;
    }

    @Override
    public Map<String, Object> upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> out = new HashMap<>();
        if (!aiFileProperties.isEnabled()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.put("code", 503);
            out.put("msg", "AI file API disabled");
            return out;
        }

        long diskFreeGb = DiskUtil.getFileStorage();
        if (diskFreeGb < 5) {
            log.error("ai-files upload refused: disk free {} GB", diskFreeGb);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.put("code", 503);
            out.put("msg", "server storage low");
            return out;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.put("code", 400);
            out.put("msg", "multipart/form-data required, field name: file");
            return out;
        }

        long maxBytes = (long) aiFileProperties.getMaxUploadMb() * 1024L * 1024L;
        DiskFileItemFactory diskFactory = DiskFileItemFactory.builder().get();
        JakartaServletFileUpload upload = new JakartaServletFileUpload(diskFactory);
        upload.setHeaderCharset(StandardCharsets.UTF_8);
        upload.setFileSizeMax(maxBytes);
        upload.setSizeMax(maxBytes + 65536);

        String filenameOverride = null;
        FileItem fileItem = null;
        try {
            @SuppressWarnings("unchecked")
            List<FileItem> fileList = upload.parseRequest(request);
            for (FileItem item : fileList) {
                if (item.isFormField() && "filename".equals(item.getFieldName())) {
                    filenameOverride = Streams.asString(item.getInputStream()).trim();
                }
            }
            for (FileItem item : fileList) {
                if (!item.isFormField() && "file".equals(item.getFieldName())) {
                    fileItem = item;
                    break;
                }
            }
        } catch (FileUploadException e) {
            log.warn("ai-files upload parse failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.put("code", 400);
            out.put("msg", e.getMessage());
            return out;
        }

        if (fileItem == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.put("code", 400);
            out.put("msg", "missing multipart field: file");
            return out;
        }

        String originalName = fileItem.getName();
        if (originalName != null && originalName.contains(File.separator)) {
            int i = originalName.lastIndexOf(File.separator);
            originalName = originalName.substring(i + 1);
        }
        if (originalName != null && originalName.contains("/")) {
            int i = originalName.lastIndexOf('/');
            originalName = originalName.substring(i + 1);
        }

        String fileName = StringUtils.hasText(filenameOverride) ? filenameOverride : originalName;
        if (!StringUtils.hasText(fileName)) {
            fileName = "upload.bin";
        }
        fileName = fileName.replace("&", "");

        String uuid = UUID.randomUUID().toString();
        String suffixName = ".bin";
        if (fileName.lastIndexOf('.') != -1) {
            suffixName = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ENGLISH);
        }
        String fileUuidName = uuid + suffixName;

        String dateDir = sanitizePath(DateUtil.getNowDate());
        String baseDir = DirUtil.getFileStoreDir();
        String dirPath = ensurePathWithinRoot(baseDir, dateDir);
        File directory = new File(dirPath);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            if (!mkdirs) {
                log.warn("mkdir failed {}", dirPath);
            }
        }
        String safeFileUuidName = sanitizePath(fileUuidName);
        File destFile = new File(dirPath, safeFileUuidName);
        String relativePath = dateDir + File.separator + safeFileUuidName;

        try (InputStream in = fileItem.getInputStream(); FileOutputStream fos = new FileOutputStream(destFile)) {
            Streams.copy(in, fos, true);
        } catch (IOException e) {
            log.error("save ai upload failed", e);
            if (destFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFile.delete();
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.put("code", 500);
            out.put("msg", "failed to save file");
            return out;
        }

        String publicBase = aiFileProperties.getPublicBaseUrl();
        String serverName;
        if (StringUtils.hasText(publicBase)) {
            serverName = publicBase.replaceFirst("^https?://", "").split("/")[0].split(":")[0];
        } else {
            serverName = request.getServerName();
        }
        FileModel fileModel = new FileModel();
        fileModel.setFilePath(relativePath);
        fileModel.setFileName(fileName);
        fileModel.setFileMd5(null);
        fileModel.setFileUuid(uuid);
        fileModel.setFileSize(destFile.length());
        fileModel.setCreateTime(new Date());
        fileModel.setServerName(serverName);

        int insert = fileMapper.insert(fileModel);
        if (insert <= 0) {
            log.error("insert failed {}", fileModel);
            //noinspection ResultOfMethodCallIgnored
            destFile.delete();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.put("code", 500);
            out.put("msg", "database insert failed");
            return out;
        }

        out.put("id", uuid);
        out.put("fileName", fileName);
        out.put("size", destFile.length());
        out.put("downloadUrl", buildDownloadUrl(request, uuid));
        out.put("code", 200);
        out.put("msg", "success");
        return out;
    }

    @Override
    public void download(String fileUuid, HttpServletRequest request, HttpServletResponse response, String range) {
        fileService.downloadByFileId(fileUuid, request, response, range);
    }

    @Override
    public Map<String, Object> meta(String fileUuid, HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();
        FileModel entity = fileMapper.selectById(fileUuid);
        if (entity == null) {
            out.put("code", 404);
            out.put("msg", "not found");
            return out;
        }
        String filePath = entity.getFilePath();
        File file = new File(DirUtil.getFileStoreDir(), filePath);
        if (!file.exists()) {
            out.put("code", 404);
            out.put("msg", "file missing on disk");
            return out;
        }
        out.put("code", 200);
        out.put("id", entity.getFileUuid());
        out.put("fileName", entity.getFileName());
        out.put("size", entity.getFileSize());
        out.put("fileMd5", entity.getFileMd5());
        out.put("createTime", entity.getCreateTime());
        out.put("downloadUrl", buildDownloadUrl(request, entity.getFileUuid()));
        return out;
    }
}
