package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.services.ShortUrlService;
import com.abitmanipulator.url_shortner.web.controller.dtos.CreateShortUrlForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HomeController {

    private final ShortUrlService shortUrlService;
    private final AppConfigProperties properties;

    public HomeController(ShortUrlService shortUrlService, AppConfigProperties properties ) {
        this.shortUrlService = shortUrlService;
        this.properties = properties;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<ShortUrlDto> shortUrls = shortUrlService.findAllPublicShortUrls();
        model.addAttribute("shortUrls", shortUrls);
        model.addAttribute("baseUrl", properties.baseUrl());
        model.addAttribute("createShortUrlForm", new CreateShortUrlForm(""));
        return "index";
    }

    @PostMapping("/short-urls")
    String createShortUrl(@ModelAttribute("createShortUrlForm") @Valid
                          CreateShortUrlForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if(bindingResult.hasErrors()) {
            List<ShortUrlDto> shortUrls = shortUrlService.findAllPublicShortUrls();
            model.addAttribute("shortUrls", shortUrls);
            model.addAttribute("baseUrl", properties.baseUrl());
            return "index";
        }
        //TODO : implement logic
        try {
            CreateShortUrlCmd cmd = new CreateShortUrlCmd(form.originalUrl());
            var shortUrlDto = shortUrlService.createShortUrl(cmd);
            redirectAttributes.addFlashAttribute("successMessage", "Short url created successfully." +
                    properties.baseUrl()+"/s" + shortUrlDto.shortKey());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Filed to create short url.");
        }
        return "redirect:/";
    }

}
