package hxy.dragon.util;

import hxy.dragon.util.file.DirUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;

public class DiskUtil {

    private static final Logger log = LoggerFactory.getLogger(DiskUtil.class);


    public static long getFileStorage() {
        String fileStoreDir = DirUtil.getFileStoreDir();
        File file = new File(fileStoreDir);
        long freeSpace = file.getFreeSpace();
        return freeSpace / 1024 / 1024 / 1024;
    }

    /**
     * 获取系统各个硬盘的总容量、已经使用的容量、剩余容量和使用率
     *
     * @throws IOException
     */
    public static long getDiskInfo() {
        long leftSpace = 0;
        try {
            DecimalFormat df = new DecimalFormat("#0.00");
            File[] disks = File.listRoots();
            for (File fileRoot : disks) {
                for (File file : fileRoot.listFiles()) {
                    log.info("====> {}", file.getName());
                }
                for (File file : fileRoot.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        String name = file.getName();
                        if ("media".equals(name)) {
                            return true;
                        }
                        return false;
                    }
                })) {
                    // 获取总容量
                    long totalSpace = file.getTotalSpace();
                    // 获取剩余容量
                    long usableSpace = file.getUsableSpace();
                    leftSpace = usableSpace;
                    // 获取已经使用的容量
                    long freeSpace = totalSpace - usableSpace;
                    // 获取使用率
                    float useRate = (float) ((freeSpace * 1.0 / totalSpace) * 100);
//                log.debug("盘符" + file.getCanonicalPath() + "   总容量： " + transformation(totalSpace) + ",已经使用： " + transformation(freeSpace) + ",剩余容量： " + transformation(usableSpace) + ",使用率： " + Double.parseDouble(df.format(useRate)) + "%   ");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return leftSpace / 1024 / 1024 / 1024;
    }

    /**
     * 将字节容量转化为GB
     */
    public static String transformation(long size) {
        return size / 1024 / 1024 / 1024 + "GB" + "   ";
    }

}
