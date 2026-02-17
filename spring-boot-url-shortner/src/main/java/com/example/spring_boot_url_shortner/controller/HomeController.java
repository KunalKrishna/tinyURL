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
    public String home() {
        return "index";
    }


    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/json")
    @ResponseBody
    /*
    * http://localhost:8080/json?param=val999&a=1&b=2
    * */
    public String handleError(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURL().toString());
        request.getParameterMap().forEach((k,v)->{
            System.out.println("key: "+k+"value:"+ Arrays.deepToString(v));
        });
        System.out.println("request :" + request.getQueryString());
        return "{ \"message\" : \"Welcome to URL Shortner\"}";
    }
    /*
    * key: paramvalue:[val999]
    * key: avalue:[1]
    * key: bvalue:[2]
    * request :param=val999&a=1&b=2
    * */
}
