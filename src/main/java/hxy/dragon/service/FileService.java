package hxy.dragon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import hxy.dragon.dao.model.FileModel;
import hxy.dragon.entity.reponse.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * @author eric
 */
@Service
public interface FileService extends IService<FileModel> {

    BaseResponse uploadGet(String identifier);

    /**
     * 上传文件
     *
     * @param request Http请求对象，包含上传文件的表单数据
     * @param response Http响应对象，用于返回上传结果
     * @return 返回BaseResponse对象，包含上传结果信息
     */
    BaseResponse uploadFile(HttpServletRequest request, HttpServletResponse response);

    BaseResponse fileMd5Check(String uuid, String md5);

    /**
     * 根据文件UUID删除文件
     *
     * @param fileUuid 文件UUID
     * @return 返回BaseResponse对象，包含操作结果信息
     */
    BaseResponse deleteFile(String fileUuid);

    BaseResponse deleteForDb(String fileUuid);

    BaseResponse deleteFileByMd5(String fileMd5);

    void downloadByFilePath(String filePath, HttpServletResponse response);

    void downloadByFileId(String fileId,HttpServletRequest request, HttpServletResponse response,String range);

    String filePageList(Model model, HttpServletRequest serverRequest);

    /**
     * 获取文件分页列表
     *
     * @param pageSize 每页显示的记录数
     * @param pageNum  当前页码
     * @param searchValue 搜索条件
     * @param serverRequest HTTP请求对象
     * @return 返回BaseResponse对象，包含分页列表信息
     */
    BaseResponse filePageList(int pageSize, int pageNum,String searchValue,HttpServletRequest serverRequest);
}
