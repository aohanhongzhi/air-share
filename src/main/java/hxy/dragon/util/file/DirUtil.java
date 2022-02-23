package hxy.dragon.util.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author eric
 * @program print-server
 * @description 目录操作工具
 * @date 2021/2/23
 */
@Component
public class DirUtil implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DirUtil.class);

    public static String getUserDir() {
        // 当前用户目录或者配置目录，再者数据库配置目录
        String property = System.getProperty("user.dir");
        return property;
    }

    /**
     * 文件存储的文件夹
     */

    private static String fileStoreDir;

    public static String getFileStoreDir() {
        return fileStoreDir;
    }

    @Value("${hxy.print.file}")
    public void setFileStoreDir(String fileStoreDir) {
        DirUtil.fileStoreDir = getUserDir() + fileStoreDir;
    }


    /**
     * 系统启动之后，开始创建相应的文件夹或者文件处理
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String fileStoreDir = DirUtil.getFileStoreDir();
        File fileFolder = new File(fileStoreDir);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
            log.info("\n====>成功创建新文件夹{}", fileStoreDir);
        }
    }
}
