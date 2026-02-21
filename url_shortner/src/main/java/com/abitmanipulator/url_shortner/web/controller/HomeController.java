package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.domain.Exception.ShortUrlNotFoundException;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.services.ShortUrlService;
import com.abitmanipulator.url_shortner.web.controller.dtos.CreateShortUrlForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class HomeController {

    private final ShortUrlService shortUrlService;
    private final AppConfigProperties properties;
    private final SecurityUtils securityUtils;

    public HomeController(ShortUrlService shortUrlService, AppConfigProperties properties, SecurityUtils securityUtils) {
        this.shortUrlService = shortUrlService;
        this.properties = properties;
        this.securityUtils = securityUtils;
    }

    // GET /?page=1&size=10&sort=createdAt,desc
    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "1")
                    Integer page,
//            @PageableDefault(page = 1, size = 10)
//            Pageable pageable,
            Model model) {
        fetchAndAddShortUrlsDataToModel(model, page);
        model.addAttribute("createShortUrlForm", new CreateShortUrlForm("", null,false));
        return "index";
    }

    private void fetchAndAddShortUrlsDataToModel(Model model, int pageNo) {
        PagedResult<ShortUrlDto> shortUrls = shortUrlService.findAllPublicShortUrls(pageNo, properties.pageSize());
        model.addAttribute("shortUrls", shortUrls);
        model.addAttribute("baseUrl", properties.baseUrl());
    }

    @PostMapping("/short-urls")
    String createShortUrl(@ModelAttribute("createShortUrlForm") @Valid
                          CreateShortUrlForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if(bindingResult.hasErrors()) {
            fetchAndAddShortUrlsDataToModel(model, 1);
            return "index";
        }
        //TODO : implement logic
        try {
            Long userId = securityUtils.getCurrentUserId();
            CreateShortUrlCmd cmd = new CreateShortUrlCmd(
                    form.originalUrl(),
                    form.expirationInDays(),
                    form.isPrivate(),
                    userId
            );
            var shortUrlDto = shortUrlService.createShortUrl(cmd);
            redirectAttributes.addFlashAttribute("successMessage", "Short url created successfully." +
                    properties.baseUrl()+"/s" + shortUrlDto.shortKey());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Filed to create short url.");
        }
        return "redirect:/";
    }

    @GetMapping("/s/{shortKey}")
    String redirectToOriginalUrl(@PathVariable("shortKey") String shortKey, Model model) throws ShortUrlNotFoundException {
        Long userId = securityUtils.getCurrentUserId();
        Optional<ShortUrlDto> shortUrlDtoOptional = shortUrlService.accessOriginalUrl(shortKey, userId);
        if(shortUrlDtoOptional.isEmpty()) {
            throw new ShortUrlNotFoundException("Invalid short key :"+ shortKey);
        }
        ShortUrlDto shortUrlDto = shortUrlDtoOptional.get();
        return "redirect:"+shortUrlDto.originalUrl();
    }

    @GetMapping("/login")
    String loginForm() {
        return "login";
    }

}
