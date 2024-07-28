package hxy.dragon.util;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * @author eric
 * @description
 * @date 2024/7/28
 */
public class AppPath {

    public static String getAppPath() {
        String classpath = null;

        // 非Spring推荐
        ProtectionDomain protectionDomain = AppPath.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();
        System.out.println("java :" + location);
        System.out.println("java classpath:" + location.getPath());

        String logPath = "";

        // 判断是否jar 包启动
        if (classpath != null && (classpath.contains("file:") || classpath.contains("nested:")) && classpath.contains(".jar") && classpath.contains("BOOT-INF")) {
            String jarFilePath = classpath.substring(0, classpath.indexOf(".jar"));
            System.out.println("jarFilePath:" + jarFilePath);
            File file = new File(jarFilePath);
            String currentPath = file.getParent();
//            String currentPath = new File(jarFilePath).getParentFile().getParentFile().getParent();
            // 如果是jar包启动的，那么获取当前jar包程序的路径，作为日志存放的位置
            if (classpath.startsWith("file:")) {
                currentPath = currentPath.replace("file:", "");
            } else if (classpath.startsWith("nested:")) {
                currentPath = currentPath.replace("nested:", "");
            } else {
                int i = currentPath.indexOf(File.separator);
                currentPath = currentPath.substring(i);
            }
            logPath = currentPath;
        }
        return logPath;
    }
}
