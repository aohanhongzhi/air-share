package hxy.dragon.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hxy.dragon.dao.mapper.FileMapper;
import hxy.dragon.dao.model.FileModel;
import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.service.FileService;
import hxy.dragon.util.DateUtil;
import hxy.dragon.util.ResponseJsonUtil;
import hxy.dragon.util.Streams;
import hxy.dragon.util.file.DirUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.ProgressListener;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 文件处理服务
 *
 * @author houxiaoyi
 */
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileModel> implements FileService {

    private static final int BUFFER_SIZE = 100 * 1024;

    @Resource
    private FileMapper fileMapper;
    
    /**
     * Sanitizes a file path to prevent directory traversal attacks
     * 
     * @param path The path to sanitize
     * @return A sanitized path without any traversal sequences
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "";
        }
        
        // Replace any backslashes with forward slashes for consistency
        String sanitized = path.replaceAll("\\\\", "/");
        
        // Remove any directory traversal sequences
        sanitized = sanitized.replaceAll("\\.\\./", "");
        sanitized = sanitized.replaceAll("\\.\\.\\\\", "");
        sanitized = sanitized.replaceAll("/\\./", "/");
        
        // Remove any remaining problematic sequences
        sanitized = sanitized.replaceAll("\\.\\.", "");
        
        // Ensure path doesn't start with / or \ to prevent absolute path usage
        sanitized = sanitized.replaceAll("^[/\\\\]+", "");
        
        return sanitized;
    }
    
    /**
     * Ensures a path is within the intended root directory
     * 
     * @param basePath The root base path
     * @param relativePath The relative path to validate
     * @return A safe canonical path that is guaranteed to be within the base path
     */
    private String ensurePathWithinRoot(String basePath, String relativePath) {
        try {
            // Normalize both paths
            Path normalizedBase = Paths.get(basePath).normalize().toAbsolutePath();
            Path normalizedPath = Paths.get(basePath, sanitizePath(relativePath)).normalize().toAbsolutePath();
            
            // Check if the normalized path starts with the base path
            if (!normalizedPath.startsWith(normalizedBase)) {
                log.error("Attempted directory traversal attack: {} is outside of {}", normalizedPath, normalizedBase);
                // If the path is outside the base, return just the base path
                return basePath;
            }
            
            return normalizedPath.toString();
        } catch (Exception e) {
            log.error("Path validation error", e);
            // In case of error, return just the base path to be safe
            return basePath;
        }
    }

    private HashMap<String, Integer[]> uploadRecordHashMap = new HashMap<>();

    @Override
    public BaseResponse uploadGet(String identifier, String fileName) {
        LambdaQueryWrapper<FileModel> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        objectLambdaQueryWrapper.eq(FileModel::getFileUuid, identifier);
        objectLambdaQueryWrapper.last("limit 1");
        FileModel fileModel = this.getOne(objectLambdaQueryWrapper);
        Map<String, Object> hashMap = new HashMap<String, Object>();
        if (fileModel == null) {
            hashMap.put("skipUpload", false);
        } else {
            // 数据库有记录，但是需要进一步检查文件是否存在
            String filePath = fileModel.getFilePath();
            File file = new File(DirUtil.getFileStoreDir(), filePath);
            if (file.exists()) {
                log.info("文件已经存在了，不接收再次上传md5 {} ,file {}", identifier, fileModel);
                hashMap.put("skipUpload", true);
                //  这里需要修改文件时间，使其排在第一位
                LambdaUpdateWrapper<FileModel> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
                lambdaUpdateWrapper.eq(FileModel::getFileUuid, identifier);
                lambdaUpdateWrapper.set(FileModel::getCreateTime, new Date());
                if (fileModel.getFileName() == null || !fileModel.getFileName().equals(fileName)) {
                    lambdaUpdateWrapper.set(FileModel::getFileName, fileName);
                    hashMap.put("newName", fileName);
                }
                fileMapper.update(lambdaUpdateWrapper);
            } else {
                log.error("实体文件不存在{} {}", file.getAbsolutePath(), fileModel);
                hashMap.put("skipUpload", false);
                QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("id", fileModel.getId());
                fileMapper.delete(queryWrapper);
            }
        }

        // 下面这个是断点续传的依据
        Integer[] integers = this.uploadRecordHashMap.get(identifier);
        hashMap.put("uploaded", integers);

        return BaseResponse.success(hashMap);
    }

    /**
     * 文件分片上传，速度很快
     * 原文链接：https://blog.csdn.net/u013248535/article/details/55823364
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    public BaseResponse uploadFile(HttpServletRequest request, HttpServletResponse response) {

        response.setContentType("application/json;charset=utf-8");

        try {
            boolean isMultipart = false;
            String contentType = request.getContentType();
            if (contentType != null) {
                isMultipart = contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart/");
            }

            if (isMultipart) {
                String fileName = "";
                String uuid = "";
                String fileUuidName = "";
                String fileMd5 = "";
                Integer chunk = 0, chunks = 0, currentChunkSize = 0;
                long chunkSize = 0;

                DiskFileItemFactory diskFactory = DiskFileItemFactory.builder().get();
                // threshold 极限、临界值，即硬盘缓存 1M
//                diskFactory.setSizeThreshold(4 * 1024);

                JakartaServletFileUpload upload = new JakartaServletFileUpload(diskFactory);
                //设置上传单个文件的大小的最大值，目前是设置为1024*1024字节，也就是100MB
                upload.setFileSizeMax(1024 * 1024 * 100);
                //设置上传文件总量的最大值，最大值=同时上传的多个文件的大小的最大值的和，目前设置为10MB
                // 设置允许上传的最大文件大小（单位MB）
                upload.setSizeMax(1024 * 1048576);
                upload.setHeaderCharset(StandardCharsets.UTF_8);
                //监听文件上传进度
                upload.setProgressListener(new ProgressListener() {
                    @Override
                    public void update(long pBytesRead, long pContentLength, int arg2) {
//                        System.out.println("文件大小为：" + pContentLength + ",当前已处理：" + pBytesRead);
                    }
                });
                String fileUrl = null;

                try {

                    // 如果下面这行代码获取的fileList为空请检查配置文件 spring.servlet.multipart.enabled=false https://www.cnblogs.com/tinya/p/9626710.html
                    @SuppressWarnings("unchecked") List<FileItem> fileList = upload.parseRequest(request);
                    boolean newUpload = false;

                    Iterator<FileItem> it = fileList.iterator();
                    while (it.hasNext()) {
                        FileItem item = it.next();
                        String name = item.getFieldName();
                        InputStream input = item.getInputStream();
                        if ("chunkSize".equals(name)) {
                            chunkSize = Long.valueOf(Streams.asString(input));
                            continue;
                        }
                        if ("currentChunkSize".equals(name)) {
                            currentChunkSize = Integer.valueOf(Streams.asString(input));
                            continue;
                        }
                        if ("uuid".equals(name)) {
                            uuid = Streams.asString(input);
                            continue;
                        } else if ("identifier".equals(name)) {
                            uuid = Streams.asString(input);
                            fileMd5 = uuid;// 这里上传的就是md5
                            continue;
                        }
                        if ("name".equals(name)) {
                            // 这个地方是依据文件流得到信息，虽然Servlet响应多次，但是死上传过来的流组成的文件是整体的。
                            // &不能在get方法中，所以下载时候可能无法找到文件
                            fileName = Streams.asString(input).replace("&", "");
                            continue;
                        } else if ("relativePath".equals(name)) {
                            // 这个地方是依据文件流得到信息，虽然Servlet响应多次，但是死上传过来的流组成的文件是整体的。
                            // &不能在get方法中，所以下载时候可能无法找到文件
                            fileName = Streams.asString(input).replace("&", "");
                            continue;
                        }


                        if ("chunk".equals(name)) {
                            chunk = Integer.valueOf(Streams.asString(input));
                            continue;
                        } else if ("chunkNumber".equals(name)) {
                            chunk = Integer.valueOf(Streams.asString(input));
                            newUpload = true;
                            continue;
                        }
                        if ("chunks".equals(name)) {
                            chunks = Integer.valueOf(Streams.asString(input));
                            continue;
                        } else if ("totalChunks".equals(name)) {
                            chunks = Integer.valueOf(Streams.asString(input));
                            newUpload = true;

                            continue;
                        }
                        // 处理上传文件内容
                        if (!item.isFormField()) {

                            Integer[] integers = this.uploadRecordHashMap.get(fileMd5);
                            if (integers == null) {
                                integers = new Integer[]{chunk};
                            } else {
                                integers = IntStream.concat(Arrays.stream(integers).mapToInt(Integer::intValue), Arrays.stream(new int[]{chunk})).boxed().toArray(Integer[]::new);
                            }

                            // 下面这个要不要put回去？
                            this.uploadRecordHashMap.put(fileMd5, integers);

                            // 文件路径一定不要用绝对路径
                            String suffixName = ".unknown";
                            if (fileName != null && !fileName.trim().isEmpty()) {
                                // 获取文件的后缀名,后缀名有可能为空
                                if (fileName.lastIndexOf(".") != -1) {
                                    suffixName = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                                }
                            }

                            fileUuidName = uuid + suffixName;

                            // TODO 重复文件存储管理，文件size获取。先要完成上传之后再次判断是否
                            // 最好的解决方案就是前端就获取文件得md5.这样后端直接以这个md5存储文件，一旦存在就不需要重复存储了。这样就需要zui去获取文件md5值
                            // 大家上传相同的课件概率还是非常大的。

                            // 目标文件

                            // 安全处理日期路径，防止目录遍历攻击
                            String dateDir = sanitizePath(DateUtil.getNowDate());
                            String baseDir = DirUtil.getFileStoreDir();
                            String dirPath = ensurePathWithinRoot(baseDir, dateDir);
                            
                            File directory = new File(dirPath);
                            if (!directory.exists()) {
                                log.info("文件夹不存在{}", dirPath);
                                boolean mkdirs = directory.mkdirs();
                                if (!mkdirs) {
                                    log.warn("文件夹新建失败 {}", dirPath);
                                }
                            }
                            // 安全处理文件路径
                            String safeFileUuidName = sanitizePath(fileUuidName);
                            File destFile = new File(dirPath, safeFileUuidName);
                            String filePath = dateDir + File.separator + safeFileUuidName;

                            if (!newUpload) {
                                // 新的上传方式不需要这个文件删除，因为 如果文件已经存在了，那么就不需要上传了，如果文件不存在，那么直接可以上传，所以也不会走到这个删除的地方。
                                // 主要是因为新的前端并发上传时候，顺序乱了。导致序号1并不是最先上传的一块。虽然去掉了并发，保证了顺序，但是按照上述业务逻辑确实已经没必要了。
                                if (chunk == 0 && destFile.exists()) {
                                    log.warn("\n====>文件已经存在了，马上删除。{}", destFile.exists());
                                    boolean delete = destFile.delete();
                                    if (!delete) {
                                        log.error("文件删除失败 {}", destFile.getAbsolutePath());
                                    }
                                    String safeUuidName = sanitizePath(fileUuidName);
                                    destFile = new File(DirUtil.getFileStoreDir(), safeUuidName);
                                }
                            }

                            // 解决文件夹上传问题 - 使用安全路径验证
                            File parentFile = destFile.getParentFile();
                            if (!parentFile.exists()) {
                                // 验证父目录路径是否安全
                                if (parentFile.getAbsolutePath().startsWith(DirUtil.getFileStoreDir())) {
                                    // 新建父级文件夹
                                    boolean mkdirs = parentFile.mkdirs();
                                    if (!mkdirs) {
                                        log.error("Could not create directory {}", parentFile);
                                    }
                                } else {
                                    log.error("Security violation: Attempted to create directory outside of allowed path: {}", parentFile);
                                }
                            }

                            if (newUpload) {
                                appendFile2(input, destFile, chunk, chunkSize, currentChunkSize);
                            } else {
                                appendFile(input, destFile, currentChunkSize);
                            }

                            if (((chunk.equals(chunks - 1)) && !newUpload) || (newUpload && chunk.equals(chunks))) {
                                this.uploadRecordHashMap.remove(fileMd5);
                                long length1 = destFile.length() / 1024 / 1024;
                                log.info("\n====>文件上传成功：{} 文件名: {} 文件大小：{} MB ", destFile.getAbsolutePath(), fileName, length1);

                                fileUrl = "file/" + filePath;


                                String serverName = request.getServerName();

                                if (fileName == null || fileName.trim().isEmpty()) {
                                    fileName = fileMd5;
                                }

                                FileModel fileModel = new FileModel();
                                fileModel.setFilePath(filePath);
                                fileModel.setFileName(fileName);
                                fileModel.setFileMd5(fileMd5);
                                fileModel.setFileUuid(uuid);
                                fileModel.setFileSize(destFile.length());
                                fileModel.setCreateTime(new Date());
                                fileModel.setServerName(serverName);
                                int insert = fileMapper.insert(fileModel);
                                if (insert <= 0) {
                                    log.error("插入数据库失败 {}", fileModel);
                                } else {
                                    log.info("成功上传文件，存入数据库{}", fileModel);
                                }

                            } else {
                                if (newUpload) {
                                    log.debug("====>当前chunk=[{}] ,还剩[" + (chunks - chunk) + "]个块文件", chunk);
                                } else {
                                    log.debug("====>当前chunk=[{}] ,还剩[" + (chunks - 1 - chunk) + "]个块文件", chunk);
                                }
                            }
                        }
                    }
                } catch (FileUploadException ex) {
                    log.warn("{}上传文件失败：", fileName, ex);
                    response.setStatus(500);
                    return BaseResponse.error("文件上传失败", ex.getMessage());
                }
                return BaseResponse.success("文件上传成功", fileUrl);
            } else {
                return BaseResponse.error("请求体异常，仅支持POST方法");
            }
        } catch (FileNotFoundException e) {
            log.warn("{}", e.getMessage(), e);
            response.setStatus(500);
            return BaseResponse.error("文件上传失败", e.getMessage());
        }catch (IOException e) {
            log.error("{}", e.getMessage(), e);
            response.setStatus(500);
            return BaseResponse.error("文件上传失败", e.getMessage());
        }
    }

    @Override
    public BaseResponse fileMd5Check(String uuid, String md5) {
        // 查询最近保存的文档中是否存在相同的md5
//        QueryWrapper<FileEntity> queryWrapper = new QueryWrapper<>();
//        // 后期优化，只用查索引存在否，不需要回表。
//        queryWrapper.eq("file_md5", md5);
//        FileEntity fileEntity = fileMapper.selectOne(queryWrapper);
//        if (fileEntity != null) {
//            return BaseResponse.success("文件已经存在");
//        } else {
//            return BaseResponse.error("没有该md5");
//        }
        FileModel fileEntity = null;
        try {
            fileEntity = fileMapper.selectById(uuid);
        } catch (Exception e) {
            log.error("参数 {} , {}", uuid, e.getMessage(), e);

            QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_uuid", uuid);
            List<FileModel> fileModels = fileMapper.selectList(queryWrapper);
            if (!fileModels.isEmpty()) {
                fileEntity = fileModels.getFirst();
            }
        }

        if (fileEntity != null) {
            String filePath = fileEntity.getFilePath();
            File file = new File(DirUtil.getFileStoreDir(), filePath);
            if (file.exists()) {
                return BaseResponse.success("实体文件存在");
            } else {
                log.error("实体文件不存在{} {}", file.getAbsolutePath(), fileEntity);
                return BaseResponse.error("实体文件不存在");
            }
        }
        log.warn("数据库没有记录，文件不存在 uuid {} md5 {} {}", uuid, md5, fileEntity);
        return BaseResponse.error("文件不存在");
    }

    @Override
    public BaseResponse deleteFileByMd5(String fileMd5) {
        QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_md5", fileMd5);
        FileModel fileModel = fileMapper.selectOne(queryWrapper);
        if (fileModel != null) {
            String fileAbsolutePath = DirUtil.getFileStoreDir() + File.separator + fileModel.getFilePath();
            File file = new File(fileAbsolutePath);
            boolean delete = file.delete();
            if (!delete) {
                log.error("删除失败！{}", fileMd5);
            } else {
                int delete1 = fileMapper.delete(queryWrapper);
                if (delete1 == 1) {
                    return BaseResponse.success();
                }

            }
        }
        return BaseResponse.error();
    }

    @Override
    public BaseResponse deleteFile(String fileUuid) {

        if (fileUuid.startsWith("[") && fileUuid.endsWith("]")) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Integer[] ids = objectMapper.readValue(fileUuid, Integer[].class);
                for (Integer id : ids) {
                    QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", id);
                    FileModel fileModel = fileMapper.selectOne(queryWrapper);
                    if (fileModel != null) {
                        deleteFile(fileModel);
                    } else {
                        log.warn("数据库没有找到这个文件 id={}", id);
                    }
                }
                return BaseResponse.success("批量删除成功");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", fileUuid);
            queryWrapper.or();
            queryWrapper.eq("file_uuid", fileUuid);
            FileModel fileModel = fileMapper.selectOne(queryWrapper);
            if (fileModel != null) {
                boolean b = deleteFile(fileModel);
                if (b) {
                    return BaseResponse.success("删除成功");
                }
            } else {
                return BaseResponse.error("数据库没有记录");
            }
        }

        return BaseResponse.error();
    }

    private boolean deleteFile(FileModel fileModel) {
        String filePath = fileModel.getFilePath();
        File file = new File(DirUtil.getFileStoreDir(), filePath);
        QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", fileModel.getId());
        if (file.exists()) {
            boolean delete = file.delete();
            if (delete) {
                fileMapper.delete(queryWrapper);
                return true;
            }
        } else {
            log.warn("文件[{}]不存在=>{}。直接从数据库删除", fileModel.getFileName(), DirUtil.getFileStoreDir() + File.separator + filePath);
            fileMapper.delete(queryWrapper);
            return true;
        }
        return false;
    }

    @Override
    public BaseResponse deleteForDb(String fileUuid) {
        QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_uuid", fileUuid);
        int delete = fileMapper.delete(queryWrapper);
        if (delete > 0) {
            return BaseResponse.success("文件删除成功");
        }
        return BaseResponse.success("文件已经删除");
    }


    @Override
    public void downloadByFilePath(String filePath, HttpServletResponse response) {

    }

    /**
     * https://blog.csdn.net/shiboyuan0410/article/details/115209291
     * https://blog.csdn.net/qq_41389354/article/details/105043312
     * 类似代码
     * https://github.com/xcbeyond/common-utils/blob/master/src/main/java/com/xcbeyond/common/file/chunk/service/impl/FileServiceImpl.java
     *
     * @param fileUuid
     * @param response
     */
    @Override
    public void downloadByFileId(String fileUuid, HttpServletRequest request, HttpServletResponse response, String range) {
        FileModel fileEntity = fileMapper.selectById(fileUuid);
        boolean fileNotExist = true;
        String originalRange = range;

        if (fileEntity != null) {
            String filePath = fileEntity.getFilePath();
            File file = new File(DirUtil.getFileStoreDir(), filePath);
            if (file.exists()) {
                fileNotExist = false;
                log.debug("current request rang:{}", range);
                //开始下载位置
                long startByte = 0;
                //结束下载位置
                long endByte = file.length() - 1;
                log.debug("文件开始位置：{}，文件结束位置：{}，文件总长度：{}", startByte, endByte, file.length());

                //有range的话
                if (range != null && range.contains("bytes=") && range.contains("-")) {
                    range = range.substring(range.lastIndexOf("=") + 1).trim();
                    String[] ranges = range.split("-");
                    try {
                        //判断range的类型
                        if (ranges.length == 1) {
                            //类型一：bytes=-2343
                            if (range.startsWith("-")) {
                                endByte = Long.parseLong(ranges[0]);
                            }
                            //类型二：bytes=2343-
                            else if (range.endsWith("-")) {
                                startByte = Long.parseLong(ranges[0]);
                            }
                        }
                        //类型三：bytes=22-2343
                        else if (ranges.length == 2) {
                            String rangeStart = ranges[0];
                            if (rangeStart != null && !rangeStart.trim().isEmpty()) {
                                startByte = Long.parseLong(rangeStart);
                            }
                            endByte = Long.parseLong(ranges[1]);
                        } else {
                            log.error("ranges错误！originalRange {} , range {}", originalRange, range);
                        }

                    } catch (NumberFormatException e) {
                        startByte = 0;
                        endByte = file.length() - 1;
                        log.error("originalRange: {} Range: {} Occur Error,Message: {}", originalRange, ranges, e.getLocalizedMessage(), e);
                    }
                }

                //要下载的长度
                long contentLength = endByte - startByte + 1;
                if (contentLength <= 0) {
                    log.error("下载文件长度异常 {},start {}，end {}", contentLength, startByte, endByte);
                }
                //文件名
                String fileName = fileEntity.getFileName();
                //文件类型。不全是application/octet-stream
                String contentType = request.getServletContext().getMimeType(fileName);

                if (contentType == null) {
                    contentType = "application/octet-stream;charset=utf-8";
                }

                // 解决下载文件时文件名乱码问题
                byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
                fileName = new String(fileNameBytes, StandardCharsets.ISO_8859_1);

                //各种响应头设置
                //支持断点续传，获取部分字节内容：
                response.setHeader("Accept-Ranges", "bytes");
                //http状态码要为206：表示获取部分内容。会导致Chrome和Edge无法下载
//                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(contentType);
//                response.setHeader("Content-Type", contentType);
                //inline表示浏览器直接使用，attachment表示下载，fileName表示下载的文件名
                response.setHeader("Content-Disposition", "inline;filename=" + fileName);
                response.setHeader("Content-Length", String.valueOf(contentLength));
                // Content-Range，格式为：[要下载的开始位置]-[结束位置]/[文件总大小]
                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());

                BufferedOutputStream outputStream = null;
                RandomAccessFile randomAccessFile = null;
                //已传送数据大小
                long transmitted = 0;
                try {
                    randomAccessFile = new RandomAccessFile(file, "r");
                    outputStream = new BufferedOutputStream(response.getOutputStream());
                    byte[] buff = new byte[4096];
                    int len = 0;
                    randomAccessFile.seek(startByte);
                    //坑爹地方四：判断是否到了最后不足4096（buff的length）个byte这个逻辑（(transmitted + len) <= contentLength）要放前面！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
                    //不然会会先读取randomAccessFile，造成后面读取位置出错，找了一天才发现问题所在
                    while ((transmitted + len) <= contentLength && (len = randomAccessFile.read(buff)) != -1) {
                        outputStream.write(buff, 0, len);
                        transmitted += len;
                    }
                    //处理不足buff.length部分
                    if (transmitted < contentLength) {
                        len = randomAccessFile.read(buff, 0, (int) (contentLength - transmitted));
                        outputStream.write(buff, 0, len);
                        transmitted += len;
                    }

                    outputStream.flush();
                    response.flushBuffer();
                    randomAccessFile.close();
                    log.debug("下载完毕：{}。 {}-{}：{}", file.getAbsolutePath(), startByte, endByte, transmitted);
                } catch (ClientAbortException e) {
                    //捕获此异常表示拥护停止下载
                    log.warn("用户停止下载：{}-{}：{}", startByte, endByte, transmitted, e);
                } catch (IOException e) {
                    log.warn("用户下载IO异常，Message：{}", e.getLocalizedMessage(), e);
                } finally {
                    try {
                        if (randomAccessFile != null) {
                            randomAccessFile.close();
                        }
                    } catch (IOException e) {
                        log.error("{}", e.getMessage(), e);
                    }
                }///end try
            } else {
                log.warn("数据库有记录，但是文件不存在。AbsoluteFilePath={}", file.getAbsoluteFile());
            }
        } else {
            log.warn("数据库没有记录，fileUuid={}", fileUuid);
        }

        if (fileNotExist) {
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ResponseJsonUtil.responseJson(response, 404, "文件没有找到", null);
        }
    }


    @Override
    public String filePageList(Model model, HttpServletRequest serverRequest) {
        String serverName = serverRequest.getServerName();
        String remoteAddr = serverRequest.getRemoteAddr();
        String code = serverRequest.getParameter("code");
        if (code == null) {
            code = serverRequest.getParameter("c");
        }
        if (log.isDebugEnabled()) {
            log.debug("remoteAddr: {}", remoteAddr);
        }

        LambdaQueryWrapper<FileModel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (serverName != null && !(serverName.startsWith("192.168") || serverName.startsWith("172.31") || serverName.startsWith("10"))) {
            log.warn("域名{}", serverName);
            lambdaQueryWrapper.eq(FileModel::getServerName, serverName);
        }
        List<FileModel> fileModels = null;
        LocalDate todaysDate = LocalDate.now();
        int dayOfMonth = todaysDate.getDayOfMonth();

        if ("newfile.cupb.top".equals(serverName)) {
            if (code != null && String.valueOf(dayOfMonth).equals(code)) {
                fileModels = fileMapper.selectList(lambdaQueryWrapper);
            }
        } else {
            fileModels = fileMapper.selectList(lambdaQueryWrapper);
        }

        model.addAttribute("fileList", fileModels);
        return "file";
    }

    @Override
    public BaseResponse filePageList(int pageSize, int pageNum, String searchValue, HttpServletRequest serverRequest) {
        String serverName = serverRequest.getServerName();
        String remoteAddr = serverRequest.getRemoteAddr();
        String code = serverRequest.getParameter("code");
        log.debug("remoteAddr: {},serverName{},code={}", remoteAddr, serverName, code);
        LambdaQueryWrapper<FileModel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (serverName != null) {
            log.warn("域名{}", serverName);
            lambdaQueryWrapper.eq(FileModel::getServerName, serverName);
        }
        IPage page = new Page(pageNum, pageSize);
        lambdaQueryWrapper.orderByDesc(FileModel::getCreateTime);

        if (searchValue != null && !"null".equals(searchValue) && !"nil".equals(searchValue) && searchValue.length() > 0) {
            lambdaQueryWrapper.like(searchValue != null && searchValue.length() > 0, FileModel::getFileName, searchValue);
        }

        IPage page1 = fileMapper.selectPage(page, lambdaQueryWrapper);

        return BaseResponse.success(page1);
    }


    /**
     * 合成文件，  这个算同步合成，可以处理并发上传！！！
     *
     * @param in
     * @param destFile         目标存储文件
     * @param offset           当前块，偏移量
     * @param chunkSize        分块的大小
     * @param currentChunkSize 当前块的大小
     */
    private void appendFile2(InputStream in, File destFile, int offset, long chunkSize, int currentChunkSize) throws IOException {
        if (currentChunkSize <= 0) {
            currentChunkSize = BUFFER_SIZE;
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(destFile, "rw")) {
            // plupload 配置了chunk的时候新上传的文件append到文件末尾
            long seek = (offset - 1) * chunkSize;
            randomAccessFile.seek(seek); // 移动到偏移量

            in = new BufferedInputStream(in, currentChunkSize);

            byte[] buffer = new byte[currentChunkSize];
            while (in.read(buffer) > 0) {
                randomAccessFile.write(buffer, 0, currentChunkSize);
            }
        } catch (Exception e) {
            // 接着网上抛，然后传输到前端
            log.error("当前块 {}=>{}", offset, chunkSize);
            throw e;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 合成文件， 这个算同步合成，不能处理并发上传！！！
     *
     * @param in
     * @param destFile
     * @param currentChunkSize 这个大小需要与前端拆分的大小一致!
     */
    private void appendFile(InputStream in, File destFile, int currentChunkSize) throws IOException {
        OutputStream out = null;
        if (currentChunkSize <= 0) {
            currentChunkSize = BUFFER_SIZE;
        }

        try {

            // plupload 配置了chunk的时候新上传的文件append到文件末尾
            if (destFile.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(destFile, true), currentChunkSize);
            } else {
                out = new BufferedOutputStream(new FileOutputStream(destFile), currentChunkSize);
            }


            in = new BufferedInputStream(in, currentChunkSize);

            int len = 0;
            byte[] buffer = new byte[currentChunkSize];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            // 接着网上抛，然后传输到前端
            throw e;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

}
