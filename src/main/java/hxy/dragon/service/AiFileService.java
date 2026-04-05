package hxy.dragon.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * AI 免登录文件上传与下载（/api/ai-files）。
 */
public interface AiFileService {

    Map<String, Object> upload(HttpServletRequest request, HttpServletResponse response) throws Exception;

    void download(String fileUuid, HttpServletRequest request, HttpServletResponse response, String range);

    Map<String, Object> meta(String fileUuid, HttpServletRequest request);
}
