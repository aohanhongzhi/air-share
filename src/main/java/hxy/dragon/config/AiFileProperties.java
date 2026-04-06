package hxy.dragon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 免登录文件接口（/api/ai-files）配置。
 */
@Data
@ConfigurationProperties(prefix = "ai.file")
public class AiFileProperties {

    /**
     * 总开关；也可通过环境变量 AI_FILE_ENABLED=0 关闭。
     */
    private boolean enabled = true;

    /**
     * 非空则上传/下载/元数据均需携带相同密钥；空则不要求密钥。
     * 优先使用环境变量 AI_FILE_UPLOAD_KEY（若已配置）。
     */
    private String uploadKey = "";

    /**
     * 返回给客户端的直链基址（不含尾斜杠），用于拼接 downloadUrl。
     */
    private String publicBaseUrl = "https://file.kmgo.top";

    /**
     * 单文件上传大小上限（MB）。
     */
    private int maxUploadMb = 100;
}
