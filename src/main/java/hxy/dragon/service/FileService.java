package hxy.dragon.service;

import hxy.dragon.entity.reponse.BaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author eric
 */
@Service
public interface FileService {
    BaseResponse uploadFile(HttpServletRequest request, HttpServletResponse response);

    BaseResponse fileMd5Check(String uuid, String md5);

    BaseResponse deleteFile(String fileUuid);

    BaseResponse deleteForDb(String fileUuid);

    BaseResponse deleteFileByMd5(String fileMd5);

    void downloadByFilePath(String filePath, HttpServletResponse response);

    void downloadByFileId(String fileId, HttpServletResponse response);

    String fileList(Model model);
}
