package hxy.dragon.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import hxy.dragon.dao.mapper.FileMapper;
import hxy.dragon.dao.model.FileModel;
import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.service.FileService;
import hxy.dragon.util.DateUtil;
import hxy.dragon.util.ResponseJsonUtil;
import hxy.dragon.util.file.DirUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

/**
 * 文件处理服务
 *
 * @author houxiaoyi
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final int BUFFER_SIZE = 100 * 1024;

    @Resource
    FileMapper fileMapper;

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
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                String fileName = "";
                String uuid = "";
                String fileUuidName = "";
                Integer chunk = 0, chunks = 0;

                DiskFileItemFactory diskFactory = new DiskFileItemFactory();
                // threshold 极限、临界值，即硬盘缓存 1M
                diskFactory.setSizeThreshold(4 * 1024);

                ServletFileUpload upload = new ServletFileUpload(diskFactory);
                //设置上传单个文件的大小的最大值，目前是设置为1024*1024字节，也就是100MB
                upload.setFileSizeMax(1024 * 1024 * 100);
                //设置上传文件总量的最大值，最大值=同时上传的多个文件的大小的最大值的和，目前设置为10MB
                // 设置允许上传的最大文件大小（单位MB）
                upload.setSizeMax(1024 * 1048576);
                upload.setHeaderEncoding("UTF-8");
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

                    Iterator<FileItem> it = fileList.iterator();
                    while (it.hasNext()) {
                        FileItem item = it.next();
                        String name = item.getFieldName();
                        InputStream input = item.getInputStream();
                        if ("uuid".equals(name)) {
                            uuid = Streams.asString(input);
                            continue;
                        }
                        if ("name".equals(name)) {
                            // 这个地方是依据文件流得到信息，虽然Servlet响应多次，但是死上传过来的流组成的文件是整体的。
                            // &不能在get方法中，所以下载时候可能无法找到文件
                            fileName = Streams.asString(input).replace("&", "");
                            continue;
                        }
                        if ("chunk".equals(name)) {
                            chunk = Integer.valueOf(Streams.asString(input));
                            continue;
                        }
                        if ("chunks".equals(name)) {
                            chunks = Integer.valueOf(Streams.asString(input));
                            continue;
                        }
                        // 处理上传文件内容
                        if (!item.isFormField()) {

                            fileUuidName = uuid + fileName;

                            // TODO 重复文件存储管理，文件size获取。先要完成上传之后再次判断是否
                            // 最好的解决方案就是前端就获取文件得md5.这样后端直接以这个md5存储文件，一旦存在就不需要重复存储了。这样就需要zui去获取文件md5值
                            // 大家上传相同的课件概率还是非常大的。

                            // 目标文件

                            String dirPath = DirUtil.getFileStoreDir() + File.separator + DateUtil.getNowDate();
                            File directory = new File(dirPath);
                            if (!directory.exists()) {
                                log.info("文件夹不存在{}", dirPath);
                                directory.mkdirs();
                            }
                            File destFile = new File(dirPath, fileUuidName);
                            String filePath = DateUtil.getNowDate() + File.separator + fileUuidName;

                            if (chunk == 0 && destFile.exists()) {
                                log.warn("\n====>文件已经存在了，马上删除。{}", destFile.exists());
                                destFile.delete();
                                destFile = new File(DirUtil.getFileStoreDir(), fileUuidName);
                            }

                            appendFile(input, destFile);

                            if (chunk == chunks - 1) {
                                long length1 = destFile.length() / 1024 / 1024;
                                log.info("\n====>文件上传成功：{} 文件大小：{} MB", destFile.getAbsolutePath(), length1);

                                fileUrl = "file/" + filePath;

                                // 文件路径一定不要用绝对路径
                                String suffixName = "unknown";
                                // 获取文件的后缀名,后缀名有可能为空
                                if (fileName.lastIndexOf(".") != -1) {
                                    suffixName = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                                }

                                FileModel fileModel = new FileModel();
                                fileModel.setFilePath(filePath);
                                fileModel.setFileName(fileName);
                                fileModel.setFileMd5(fileUuidName);
                                fileModel.setFileUuid(uuid);
                                fileModel.setFileSize(length1);
                                fileMapper.insert(fileModel);

                            } else {
//                                log.debug("\n====>还剩[" + (chunks - 1 - chunk) + "]个块文件");
                            }
                        }
                    }
                } catch (FileUploadException ex) {
                    log.warn(fileName + "上传文件失败：" + ex.getMessage());
                    return BaseResponse.error("文件上传失败", ex.getMessage());
                }
                return BaseResponse.success("文件上传成功", fileUrl);
            } else {
                return BaseResponse.error("请求体异常，仅支持POST方法");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
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
        return BaseResponse.success();
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
        QueryWrapper<FileModel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_uuid", fileUuid);
        FileModel fileModel = fileMapper.selectOne(queryWrapper);
        if (fileModel != null) {
            String filePath = fileModel.getFilePath();
            File file = new File(DirUtil.getFileStoreDir(), filePath);
            if (file.exists()) {
                boolean delete = file.delete();
                if (delete){
                    fileMapper.delete(queryWrapper);
                }
            }
        }
        return BaseResponse.success();
    }


    @Override
    public void downloadByFilePath(String filePath, HttpServletResponse response) {

    }

    /**
     * https://blog.csdn.net/shiboyuan0410/article/details/115209291
     *
     * @param fileUuid
     * @param response
     */
    @Override
    public void downloadByFileId(String fileUuid, HttpServletResponse response) {
        FileModel fileEntity = fileMapper.selectById(fileUuid);
        if (fileEntity != null) {
            String filePath = fileEntity.getFilePath();
            File file = new File(DirUtil.getFileStoreDir(), filePath);
            if (file.exists()) {
                log.debug("\n====>下载文件：" + file);
                // 告诉浏览器输出内容为流
                response.setContentType("application/octet-stream");
                // 设置响应头，控制浏览器下载该文件
                try {
                    response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(fileEntity.getFileName(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    log.error("{}", e.getMessage(), e);
                }

                try (FileInputStream fileInputStream = new FileInputStream(file); BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream())) {
                    FileCopyUtils.copy(bufferedInputStream, bufferedOutputStream);
                    return;
                } catch (Exception e) {
                    log.error("下载异常", e);
                } finally {
                }
            }
        }

        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        ResponseJsonUtil.responseJson(response, 404, "文件没有找到", null);
    }

    @Override
    public String fileList(Model model) {
        List<FileModel> fileModels = fileMapper.selectList(null);
        model.addAttribute("fileList", fileModels);
        return "file";
    }

    /**
     * 合成文件
     *
     * @param in
     * @param destFile
     */
    private void appendFile(InputStream in, File destFile) {
        OutputStream out = null;
        try {
            // plupload 配置了chunk的时候新上传的文件append到文件末尾
            if (destFile.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(destFile, true), BUFFER_SIZE);
            } else {
                out = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
            }
            in = new BufferedInputStream(in, BUFFER_SIZE);

            int len = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            log.error("", e);
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
