package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.config.WebSecurityConfig;
import com.abitmanipulator.url_shortner.domain.models.CreateShortUrlCmd;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import com.abitmanipulator.url_shortner.domain.models.ShortUrlDto;
import com.abitmanipulator.url_shortner.services.ShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web slice: only MVC + Thymeleaf + our real security rules (imported) are started; the service
 * layer is mocked, so these tests check routing, validation, views and access control.
 */
@WebMvcTest(HomeController.class)
@Import(WebSecurityConfig.class)
// @ConfigurationPropertiesScan on UrlShortnerApp is not applied in slices, so register the app.* properties explicitly
@EnableConfigurationProperties(AppConfigProperties.class)
class HomeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ShortUrlService shortUrlService;
    @MockitoBean
    SecurityUtils securityUtils;

    private static final PagedResult<ShortUrlDto> EMPTY_PAGE =
            new PagedResult<>(List.of(), 1, 0, 0, true, true, false, false);

    private static ShortUrlDto dto(String key, String url) {
        return new ShortUrlDto(1L, key, url, false, null, null, 0L, Instant.now());
    }

    @BeforeEach
    void setUp() {
        // anonymous by default. Without this the mock would return 0L (Mockito's default for Long), not null
        when(securityUtils.getCurrentUserId()).thenReturn(null);
        when(shortUrlService.findAllPublicShortUrls(anyInt(), anyInt())).thenReturn(EMPTY_PAGE);
        when(shortUrlService.getUserShortUrls(any(), anyInt(), anyInt())).thenReturn(EMPTY_PAGE);
    }

    @Test
    void homePageIsPublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("shortUrls", "createShortUrlForm", "baseUrl"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
    }

    @Test
    void createShortUrlShowsTheShortLink() throws Exception {
        when(shortUrlService.createShortUrl(any())).thenReturn(dto("abc123", "https://example.com"));

        mvc.perform(post("/short-urls").param("originalUrl", "https://example.com").param("isPrivate", "false"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("successMessage",
                        "Short url created successfully. http://localhost:8080/s/abc123"));
    }

    @Test
    void createShortUrlWithoutPrivacyFlagDefaultsToPublic() throws Exception {
        // a client that omits the checkbox entirely (no hidden "_isPrivate" field) must not crash
        when(shortUrlService.createShortUrl(any())).thenReturn(dto("abc123", "https://example.com"));

        mvc.perform(post("/short-urls").param("originalUrl", "https://example.com"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(shortUrlService).createShortUrl(new CreateShortUrlCmd("https://example.com", null, false, null));
    }

    @Test
    void blankUrlIsAValidationError() throws Exception {
        mvc.perform(post("/short-urls").param("originalUrl", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("createShortUrlForm", "originalUrl"));
        verify(shortUrlService, never()).createShortUrl(any());
    }

    @Test
    void expiryOutOfRangeIsAValidationError() throws Exception {
        mvc.perform(post("/short-urls").param("originalUrl", "https://example.com").param("expirationInDays", "366"))
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("createShortUrlForm", "expirationInDays"));
    }

    @Test
    void serviceFailureIsShownAsFlashError() throws Exception {
        when(shortUrlService.createShortUrl(any())).thenThrow(new RuntimeException("Invalid URL"));

        mvc.perform(post("/short-urls").param("originalUrl", "https://nope.invalid"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "Failed to create short url."));
    }

    @Test
    void shortKeyRedirectsToOriginalUrl() throws Exception {
        when(shortUrlService.accessOriginalUrl("abc123", null)).thenReturn(Optional.of(dto("abc123", "https://example.com")));

        mvc.perform(get("/s/abc123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com"));
    }

    @Test
    void unknownShortKeyIs404() throws Exception {
        when(shortUrlService.accessOriginalUrl("nope", null)).thenReturn(Optional.empty());

        mvc.perform(get("/s/nope"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void myUrlsRequiresLogin() throws Exception {
        mvc.perform(get("/my-urls"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void myUrlsShowsOnlyTheCurrentUsersUrls() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        mvc.perform(get("/my-urls").with(user("siva@gmail.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("my-urls"));
        verify(shortUrlService).getUserShortUrls(eq(2L), eq(1), anyInt());
    }

    @Test
    void deleteWithoutSelectionShowsError() throws Exception {
        mvc.perform(post("/delete-urls").with(user("siva@gmail.com").roles("USER")))
                .andExpect(redirectedUrl("/my-urls"))
                .andExpect(flash().attribute("errorMessage", "No URLs selected for deletion"));
    }

    @Test
    void deleteRemovesOnlyTheCurrentUsersUrls() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        mvc.perform(post("/delete-urls").param("ids", "5", "6").with(user("siva@gmail.com").roles("USER")))
                .andExpect(redirectedUrl("/my-urls"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(shortUrlService).deleteUserShortUrls(List.of(5L, 6L), 2L);
    }

    @Test
    void adminInheritsUserRoleForDelete() throws Exception {
        // ROLE_ADMIN > ROLE_USER: the role hierarchy must also apply to @PreAuthorize
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        mvc.perform(post("/delete-urls").param("ids", "5").with(user("admin@gmail.com").roles("ADMIN")))
                .andExpect(flash().attributeExists("successMessage"));
        verify(shortUrlService).deleteUserShortUrls(List.of(5L), 1L);
    }
}
