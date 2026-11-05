package com.abitmanipulator.url_shortner.web.controller;

import com.abitmanipulator.url_shortner.config.WebSecurityConfig;
import com.abitmanipulator.url_shortner.domain.models.CreateUserCmd;
import com.abitmanipulator.url_shortner.domain.models.Role;
import com.abitmanipulator.url_shortner.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@Import(WebSecurityConfig.class)
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @Test
    void registerFormIsPublic() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"));
    }

    @Test
    void registersAsRegularUser() throws Exception {
        mvc.perform(post("/register")
                        .param("email", "new@example.com").param("password", "pw").param("name", "New"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
        // self-registration can never create an admin
        verify(userService).createUser(new CreateUserCmd("new@example.com", "pw", "New", Role.ROLE_USER));
    }

    @Test
    void invalidEmailIsAValidationError() throws Exception {
        mvc.perform(post("/register")
                        .param("email", "not-an-email").param("password", "pw").param("name", "New"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));
        verify(userService, never()).createUser(any());
    }

    @Test
    void duplicateEmailIsShownAsFlashError() throws Exception {
        doThrow(new RuntimeException("Email already exists")).when(userService).createUser(any());

        mvc.perform(post("/register")
                        .param("email", "siva@gmail.com").param("password", "pw").param("name", "Siva"))
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", "Registration failed: Email already exists"));
    }
}
