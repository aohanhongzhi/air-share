package hxy.dragon.util.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author eric
 * @program print-server
 * @description 目录操作工具
 * @date 2021/2/23
 */
@Component
public class DirUtil implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DirUtil.class);

    @Value("${spring.profiles.active}")
    String activeProfiles;

    public static String getUserDir() {
        // 当前用户目录或者配置目录，再者数据库配置目录
        String property = System.getProperty("user.dir");
        return property;
    }

    /**
     * 文件存储的文件夹
     */

    private static String deaultFileStoreDir = getUserDir() + File.separator + "file";
    private static String fileStoreDir;

    public static String getFileStoreDir() {
        return fileStoreDir;
    }

    @Value("${hxy.print.absolute-file-path}")
    public void setFileStoreDir(String fileStoreDir) {
        if ("prod".equals(activeProfiles) || "beta".equals(activeProfiles)) {
            if (fileStoreDir != null && fileStoreDir.trim().length() > 0) {
                DirUtil.fileStoreDir = fileStoreDir;
            } else {
                DirUtil.fileStoreDir = deaultFileStoreDir;
            }
        } else {
            DirUtil.fileStoreDir = deaultFileStoreDir;
        }
    }


    /**
     * 系统启动之后，开始创建相应的文件夹或者文件处理
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String fileStoreDir = DirUtil.getFileStoreDir();
        String currentDir = fileStoreDir;
        File fileFolder = new File(fileStoreDir);
        if (!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            if (mkdirs) {
                log.info("\n====>成功创建新文件夹{}", currentDir);
            } else {
                currentDir = deaultFileStoreDir;
                fileFolder = new File(deaultFileStoreDir);
                if (!fileFolder.exists()) {
                    mkdirs = fileFolder.mkdirs();
                    if (mkdirs) {
                        log.info("\n====>成功创建新文件夹{}", currentDir);
                    } else {
                        throw new FileNotFoundException("文件夹创建失败");
                    }
                }
            }
        }
        log.info("\n====>当前存储文件夹 {}", currentDir);
    }
}
