package hxy.dragon.controller;

import hxy.dragon.dao.model.FileEntity;
import hxy.dragon.util.file.DirUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Controller
public class IndexController {

    @RequestMapping(path = {"/index", "/"})
    public String index() {
        return "index";
    }

    @RequestMapping("/test")
    public String test() {
        return "hello";
    }

    @RequestMapping("/file")
    public String file(Model model) {
        List<FileEntity> list = new ArrayList<>();

        File file = new File(DirUtil.getFileStoreDir());
        File[] fs = file.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) {
                String name = f.getName();
                File[] files = f.listFiles();
                for (File f1 : files) {
                    String fileName = f1.getName();
                    String fileUrl = "file/" + name + "/" + fileName;
                    FileEntity fileEntity = new FileEntity();
                    if (fileName.startsWith("o_1f")) {
                        fileName = fileName.substring(30);
                    }
                    fileEntity.setFileName(fileName);
                    fileEntity.setFileSize(f1.length());
                    fileEntity.setFilePath(fileUrl);
                    list.add(fileEntity);
                }
            } else {
                FileEntity fileEntity = new FileEntity();
                fileEntity.setFileName(f.getName());
                fileEntity.setFileSize(f.length());
                list.add(fileEntity);
            }
        }

        model.addAttribute("fileList", list);
        return "file";
    }
}
