package hxy.dragon.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @version 1.0
 * @class: CustomLogContextListener
 * @description: 定义logback 日志监听器，指定日志文件存放目录
 */
public class CustomLogContextListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {


    /**
     * 存储日志路径标识
     */
    public static final String LOG_PAHT_KEY = "LOG_PATH";

    @Override
    public boolean isResetResistant() {
        return false;
    }

    @Override
    public void onStart(LoggerContext loggerContext) {

    }

    @Override
    public void onReset(LoggerContext loggerContext) {

    }

    @Override
    public void onStop(LoggerContext loggerContext) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {

    }

    @Override
    public void start() {
        String classpath = null;
        try {
            classpath = ResourceUtils.getURL(ResourceUtils.CLASSPATH_URL_PREFIX).getPath();
        } catch (FileNotFoundException e) {
            System.err.println("File not found" + e.getMessage());
        }

        String logPath = "./logs";

        // 判断是否jar 包启动
        if (classpath != null && classpath.contains("jar!")) {
            System.out.println("classpath:" + classpath);
            String currentPath = new File(classpath).getParentFile().getParentFile().getParent();
            // 如果是jar包启动的，那么获取当前jar包程序的路径，作为日志存放的位置
            currentPath = currentPath.replace("file:", "");
            logPath = currentPath + File.separator + "logs";
        }

        // 判断文件夹是否存在，不存在需要新建
        File file = new File(logPath);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (mkdirs) {
                System.out.println("日志存储路径创建成功 " + logPath);
            }
        }

        System.out.println("日志存储路径 " + logPath);
        System.setProperty(LOG_PAHT_KEY, logPath);
        Context context = getContext();
        context.putProperty(LOG_PAHT_KEY, logPath);
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }
}