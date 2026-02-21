package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.domain.Exception.ShortUrlNotFoundException;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.services.ShortUrlService;
import com.abitmanipulator.url_shortner.web.controller.dtos.CreateShortUrlForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
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
        model.addAttribute("paginationUrl", "/my-urls");
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

    @GetMapping("/my-urls")
    @PreAuthorize("isAuthenticated()")
    public String showUserUrls(
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        var currentUserId = securityUtils.getCurrentUserId();
        PagedResult<ShortUrlDto> myUrls =
                shortUrlService.getUserShortUrls(currentUserId, page, properties.pageSize());
        model.addAttribute("shortUrls", myUrls);
        model.addAttribute("baseUrl", properties.baseUrl());
        model.addAttribute("paginationUrl", "/my-urls");
        return "my-urls";
    }

    @PostMapping("/delete-urls")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR')")
    public String deleteUrls(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "No URLs selected for deletion");
            return "redirect:/my-urls";
        }
        try {
            var currentUserId = securityUtils.getCurrentUserId();
            shortUrlService.deleteUserShortUrls(ids, currentUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Selected URLs have been deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting URLs: " + e.getMessage());
        }
        return "redirect:/my-urls";
    }
}
