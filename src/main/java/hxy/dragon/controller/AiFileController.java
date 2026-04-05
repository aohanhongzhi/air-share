package hxy.dragon.controller;

import hxy.dragon.config.AiFileProperties;
import hxy.dragon.service.AiFileService;
import hxy.dragon.util.ResponseJsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI / 脚本免登录文件接口（可选静态密钥）。与 {@code docs/ai-file-api.md} 约定一致。
 */
@CrossOrigin
@RestController
@RequestMapping("/api/ai-files")
@Slf4j
public class AiFileController {

    @Autowired
    private AiFileService aiFileService;

    @Autowired
    private AiFileProperties aiFileProperties;

    @Autowired
    private Environment environment;

    /**
     * 解析有效上传密钥：环境变量 AI_FILE_UPLOAD_KEY 优先于配置 ai.file.upload-key。
     */
    private String configuredKey() {
        String envKey = environment.getProperty("AI_FILE_UPLOAD_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey.trim();
        }
        String k = aiFileProperties.getUploadKey();
        return k == null ? "" : k.trim();
    }

    private boolean keyMatches(HttpServletRequest request) {
        String required = configuredKey();
        if (!StringUtils.hasText(required)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            if (required.equals(token)) {
                return true;
            }
        }
        String headerKey = request.getHeader("X-AI-File-Key");
        if (required.equals(headerKey != null ? headerKey.trim() : "")) {
            return true;
        }
        String q = request.getParameter("key");
        return required.equals(q != null ? q.trim() : "");
    }

    private Map<String, Object> unauthorized() {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 401);
        m.put("msg", "Unauthorized");
        return m;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!keyMatches(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return unauthorized();
        }
        return aiFileService.upload(request, response);
    }

    @GetMapping("/{id}")
    public void download(@PathVariable("id") String id,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         @RequestHeader(name = "Range", required = false) String range) {
        if (!keyMatches(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ResponseJsonUtil.responseJson(response, 401, "Unauthorized", null);
            return;
        }
        aiFileService.download(id, request, response, range);
    }

    @GetMapping("/{id}/meta")
    public Map<String, Object> meta(@PathVariable("id") String id,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (!keyMatches(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return unauthorized();
        }
        Map<String, Object> m = aiFileService.meta(id, request);
        Object code = m.get("code");
        if (code instanceof Number) {
            int c = ((Number) code).intValue();
            if (c == 404) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        return m;
    }
}
