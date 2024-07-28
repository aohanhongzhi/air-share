package hxy.dragon.config;

import hxy.dragon.util.AppPath;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @description bean初始化之前，Spring环境变量修改 https://blog.csdn.net/weixin_43827985/article/details/114368232
 * 1. 可以读取Azue KeyValue
 * 2. 可以读取本机的配置文件
 * <p>
 * 这个类生效靠：app/src/main/resources/META-INF/spring.factories
 * @date 2022/2/25
 */
@Component
//@Profile(value = {"prod", "beta"}) // 貌似这个没啥用，其他环境变量的时候也跑了程序
public class RemoteEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String currentProfile = environment.getProperty("spring.profiles.active");

        final Properties properties = new Properties();
        String url = "jdbc:sqlite:" + AppPath.getAppPath() + "airshare.db";
        System.out.println("数据库目录 " + url);

        properties.put("spring.datasource.url", url);
        PropertiesPropertySource propertySource = new PropertiesPropertySource(currentProfile, properties);
        environment.getPropertySources().addFirst(propertySource);

    }
}
