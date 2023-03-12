package hxy.dragon.controller;

import hxy.dragon.entity.reponse.BaseResponse;
import hxy.dragon.service.FileService;
import hxy.dragon.util.DiskUtil;
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
import java.util.HashMap;
import java.util.Map;

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
        long diskinfo = DiskUtil.getDiskInfo();
        log.info("upload disk free size  {}", diskinfo);
        if (diskinfo < 5) {
            // 经过实际测试这个获取的是系统盘符的大小，不是数据盘的大小。但是能有效检测系统存储满了导致崩溃。
            // 防止恶意上传导致服务器崩了（可能阻止不了）
            return BaseResponse.error("服务器空间不足了");
        } else {
            return fileService.uploadFile(request, response);
        }
    }

    @GetMapping("/file/upload")
    public BaseResponse uploadGet(HttpServletRequest request, HttpServletResponse response) {
        long diskinfo = DiskUtil.getDiskInfo();
        log.info("upload disk free size  {}", diskinfo);
        if (diskinfo < 5) {
            // 经过实际测试这个获取的是系统盘符的大小，不是数据盘的大小。但是能有效检测系统存储满了导致崩溃。
            // 防止恶意上传导致服务器崩了（可能阻止不了）
            return BaseResponse.error("服务器空间不足了");
        } else {
//            return fileService.uploadFile(request, response);
            Map<String,Object> hashMap = new HashMap<String,Object>();
            hashMap.put("skipUpload",false);
            hashMap.put("uploaded",0);
            return BaseResponse.success(hashMap);
        }
    }

    @DeleteMapping("/file/delete")
    public BaseResponse delete(@RequestBody String fileUuid) {
        return fileService.deleteFile(fileUuid);
    }

    @GetMapping("/file/list")
    public BaseResponse listFile(int pageSize, int pageNum) {
        return fileService.filePageList(pageSize, pageNum);
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
    public void download(@RequestParam("fileUuid") String fileUuid, HttpServletRequest request, HttpServletResponse
            response,// 获取Header里面的Range内容, 可选项, 可为空
                         @RequestHeader(name = "Range", required = false) String range) {
        fileService.downloadByFileId(fileUuid, request, response, range);
    }
}
