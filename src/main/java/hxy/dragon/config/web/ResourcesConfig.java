package hxy.dragon.config.web;

import hxy.dragon.util.file.DirUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author eric
 * @program air-share
 * @description
 * @date 2022/2/24
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** 本地文件上传路径 */
        registry.addResourceHandler("/file/**").addResourceLocations("file:" + DirUtil.getFileStoreDir() + "/");
    }
}
