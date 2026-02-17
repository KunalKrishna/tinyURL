package com.example.spring_boot_url_shortner.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Spring Boot Url Shortner - Thymeleaf");
        model.addAttribute("var", "test");
        model.addAttribute("var2", "test2");
        return "index";
    }


    @GetMapping("/about")
    public String about() {
        return "about";
    }

}
