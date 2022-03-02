package hxy.dragon.controller;

import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public BaseResponse delete(String fileName) {
        return fileService.deleteFile(fileName);
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
    @Deprecated
    public void download(@RequestParam("filePath") String filePath) {

    }
}
