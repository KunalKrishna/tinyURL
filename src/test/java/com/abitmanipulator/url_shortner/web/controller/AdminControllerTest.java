package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.AppConfigProperties;
import com.abitmanipulator.url_shortner.config.WebSecurityConfig;
import com.abitmanipulator.url_shortner.domain.models.PagedResult;
import com.abitmanipulator.url_shortner.services.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
// @ConfigurationPropertiesScan on UrlShortnerApp is not applied in slices, so register the app.* properties explicitly
@EnableConfigurationProperties(AppConfigProperties.class)
class AdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ShortUrlService shortUrlService;

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void regularUserIsForbidden() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user("siva@gmail.com").roles("USER")))
                .andExpect(status().isForbidden());
        verify(shortUrlService, never()).findAllShortUrls(anyInt(), anyInt());
    }

    @Test
    void adminSeesDashboard() throws Exception {
        when(shortUrlService.findAllShortUrls(anyInt(), anyInt()))
                .thenReturn(new PagedResult<>(List.of(), 1, 0, 0, true, true, false, false));

        mvc.perform(get("/admin/dashboard").with(user("admin@gmail.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
    }
}
