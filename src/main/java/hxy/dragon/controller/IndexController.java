package hxy.dragon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
