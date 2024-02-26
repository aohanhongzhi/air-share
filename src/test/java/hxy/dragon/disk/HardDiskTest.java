package hxy.dragon.disk;

import hxy.dragon.util.DiskUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardDiskTest {

    private static final Logger log = LoggerFactory.getLogger(HardDiskTest.class);


    @Test
    public void testA(){
        DiskUtil.getDiskInfo();
    }

    @Test
    public void testB(){
        long fileStorage = DiskUtil.getFileStorage();
        log.info("{}",fileStorage);
    }

}
