package com.abitmanipulator.url_shortner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flows through every layer (security → controller → service → JPA/JDBC → Postgres).
 * Nothing is mocked and nothing is rolled back, so each test uses unique URLs/emails.
 * `user("siva@gmail.com")` skips the login form but SecurityUtils still resolves the real DB user by email.
 */
class ShortUrlFlowIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;

    private static String uniqueUrl() {
        return "https://example.com/it/" + UUID.randomUUID();
    }

    private String shortKeyOf(String originalUrl) {
        return jdbc.sql("SELECT short_key FROM short_urls WHERE original_url = ?")
                .param(originalUrl).query(String.class).single();
    }

    private long clickCountOf(String shortKey) {
        return jdbc.sql("SELECT click_count FROM short_urls WHERE short_key = ?")
                .param(shortKey).query(Long.class).single();
    }

    @Test
    void anonymousCreatesUrlThenAnyoneCanFollowIt() throws Exception {
        String url = uniqueUrl();

        mvc.perform(post("/short-urls").param("originalUrl", url).param("isPrivate", "true"))
                .andExpect(redirectedUrl("/"));
        String key = shortKeyOf(url);

        mvc.perform(get("/s/" + key)).andExpect(redirectedUrl(url));
        mvc.perform(get("/s/" + key)).andExpect(redirectedUrl(url));

        assertThat(clickCountOf(key)).isEqualTo(2);
        // anonymous links are always public, whatever the form said
        assertThat(jdbc.sql("SELECT is_private FROM short_urls WHERE short_key = ?").param(key)
                .query(Boolean.class).single()).isFalse();
    }

    @Test
    void privateUrlOpensOnlyForItsOwner() throws Exception {
        String url = uniqueUrl();
        mvc.perform(post("/short-urls").param("originalUrl", url).param("isPrivate", "true")
                        .with(user("siva@gmail.com").roles("USER")))
                .andExpect(redirectedUrl("/"));
        String key = shortKeyOf(url);

        mvc.perform(get("/s/" + key).with(user("siva@gmail.com").roles("USER"))).andExpect(redirectedUrl(url));
        mvc.perform(get("/s/" + key).with(user("admin@gmail.com").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/s/" + key)).andExpect(status().isNotFound());

        assertThat(clickCountOf(key)).isEqualTo(1); // denied attempts are not counted
    }

    @Test
    void userCannotDeleteSomeoneElsesUrl() throws Exception {
        String url = uniqueUrl();
        mvc.perform(post("/short-urls").param("originalUrl", url).with(user("admin@gmail.com").roles("ADMIN")));
        Long adminUrlId = jdbc.sql("SELECT id FROM short_urls WHERE original_url = ?").param(url)
                .query(Long.class).single();

        mvc.perform(post("/delete-urls").param("ids", adminUrlId.toString()).with(user("siva@gmail.com").roles("USER")))
                .andExpect(redirectedUrl("/my-urls"));

        assertThat(jdbc.sql("SELECT count(*) FROM short_urls WHERE id = ?").param(adminUrlId)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void adminDashboardIsForAdminsOnly() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user("siva@gmail.com").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("admin@gmail.com").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void seededUsersCanLogInWithTheirBcryptPasswords() throws Exception {
        mvc.perform(formLogin("/login").user("siva@gmail.com").password("secret"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("siva@gmail.com").withRoles("USER"));

        mvc.perform(formLogin("/login").user("siva@gmail.com").password("wrong"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void registeredUserCanLogIn() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/register").param("email", email).param("password", "pw123").param("name", "IT User"))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(formLogin("/login").user(email).password("pw123"))
                .andExpect(authenticated().withUsername(email).withRoles("USER"));
    }
}
