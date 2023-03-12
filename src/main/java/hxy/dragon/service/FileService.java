package hxy.dragon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import hxy.dragon.dao.model.FileModel;
import hxy.dragon.entity.reponse.BaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author eric
 */
@Service
public interface FileService extends IService<FileModel> {
    BaseResponse uploadFile(HttpServletRequest request, HttpServletResponse response);

    BaseResponse fileMd5Check(String uuid, String md5);

    BaseResponse deleteFile(String fileUuid);

    BaseResponse deleteForDb(String fileUuid);

    BaseResponse deleteFileByMd5(String fileMd5);

    void downloadByFilePath(String filePath, HttpServletResponse response);

    void downloadByFileId(String fileId,HttpServletRequest request, HttpServletResponse response,String range);

    String filePageList(Model model, HttpServletRequest serverRequest);

    BaseResponse filePageList(int pageSize, int pageNum);
}
