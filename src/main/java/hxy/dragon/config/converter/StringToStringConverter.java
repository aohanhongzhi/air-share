package hxy.dragon.config.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * 这个转换器，可以去掉参数里的null值
 */
public class StringToStringConverter implements Converter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(StringToStringConverter.class);

    @Override
    public String convert(String source) {
        log.info("convert source: {}", source);
        if (source != null && ("null".equals(source.toLowerCase()) || "nil".equals(source))) {
            return "";
        }
        return source;
    }
}
