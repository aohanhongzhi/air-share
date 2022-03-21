package hxy.dragon.controller;

import hxy.dragon.dao.model.FileModel;
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
        List<FileModel> list = new ArrayList<>();

        File file = new File(DirUtil.getFileStoreDir());
        File[] fs = file.listFiles();
        if (fs != null && fs.length > 0) {
            for (File f : fs) {
                if (f.isDirectory()) {
                    String name = f.getName();
                    File[] files = f.listFiles();
                    for (File f1 : files) {
                        String fileName = f1.getName();
                        String fileUrl = "file/" + name + "/" + fileName;
                        FileModel fileModel = new FileModel();
                        if (fileName.startsWith("o_1f")) {
                            fileName = fileName.substring(30);
                        }
                        fileModel.setFileName(fileName);
                        fileModel.setFileSize(f1.length());
                        fileModel.setFilePath(fileUrl);
                        list.add(fileModel);
                    }
                } else {
                    FileModel fileModel = new FileModel();
                    fileModel.setFileName(f.getName());
                    fileModel.setFileSize(f.length());
                    list.add(fileModel);
                }
            }
        }

        model.addAttribute("fileList", list);
        return "file";
    }
}
