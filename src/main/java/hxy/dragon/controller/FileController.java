package hxy.dragon.controller;

import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@CrossOrigin
@RestController
@Slf4j
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 文档上传,测试在局域网下最大上传23GB文件都没有问题，电脑16GB内存的。
     * 这里的方法必须是POST
     */
    @PostMapping("/file/upload")
    public BaseResponse upload(HttpServletRequest request, HttpServletResponse response) {
        return fileService.uploadFile(request, response);
    }

    @DeleteMapping("/file/delete")
    public BaseResponse delete(@RequestBody String fileUuid) {
        return fileService.deleteFile(fileUuid);
    }

    /**
     * 把数据从数据库删除
     *
     * @param fileUuid
     * @return
     */
    @DeleteMapping("/file/deleteForDb")
    public BaseResponse deleteForDb(String fileUuid) {
        return fileService.deleteForDb(fileUuid);
    }

    /**
     * 文档查询是否已经有相同的了
     */
    @GetMapping("/api/file/exsit")
    public BaseResponse exsit(String uuid, String md5) {
        return fileService.fileMd5Check(uuid, md5);
    }


    /**
     * 文件下载，直接文件地址
     */
    @GetMapping("/file/download")
    public void download(@RequestParam("fileUuid") String fileUuid, HttpServletRequest request, HttpServletResponse response,// 获取Header里面的Range内容, 可选项, 可为空
                         @RequestHeader(name = "Range", required = false) String range) {
        fileService.downloadByFileId(fileUuid, request, response, range);
    }
}
