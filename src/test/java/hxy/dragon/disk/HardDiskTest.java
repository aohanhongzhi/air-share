package hxy.dragon.disk;

import hxy.dragon.util.DiskUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HardDiskTest {


    @Test
    public void testA(){
        try {
            DiskUtil.getDiskInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
