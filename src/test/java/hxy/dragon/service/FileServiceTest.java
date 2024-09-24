package hxy.dragon.service;

import hxy.dragon.AirShareApplicationTests;
import hxy.dragon.entity.reponse.BaseResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class FileServiceTest extends AirShareApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(FileServiceTest.class);

    @Autowired
    FileService fileService;

    @Test
    public void testSelectFile() {
        BaseResponse baseResponse = fileService.fileMd5Check("", "");
        log.info("File {}", baseResponse);
    }

    @Test
    public void deleteFile(){
        fileService.deleteFile("11");
    }

}
