package hxy.dragon.service;

import hxy.dragon.entity.reponse.BaseResponse;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * @author eric
 */
@Service
public interface FileService {
    BaseResponse uploadFile(HttpServletRequest request, HttpServletResponse response);

    BaseResponse fileMd5Check(String uuid,String md5);

    BaseResponse deleteFile(String fileNam);


    void downloadByFilePath(String filePath, HttpServletResponse response);

    void downloadByFileId(String fileId, HttpServletResponse response) throws UnsupportedEncodingException, FileNotFoundException;

}
