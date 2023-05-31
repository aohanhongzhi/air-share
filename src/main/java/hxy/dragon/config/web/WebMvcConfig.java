package hxy.dragon.config.web;

import hxy.dragon.config.converter.StringToStringConverter;
import hxy.dragon.util.file.DirUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * MVC通用配置
 * </p>
 *
 * @description: MVC通用配置
 * @version: V1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** 本地文件上传路径 */
        registry.addResourceHandler("/file/**").addResourceLocations("file:" + DirUtil.getFileStoreDir() + "/");
    }

    /**
     * 转换器工厂 addConverterFactory
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToStringConverter());
    }
}