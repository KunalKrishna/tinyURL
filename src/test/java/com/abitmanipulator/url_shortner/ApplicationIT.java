package com.abitmanipulator.url_shortner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Smoke tests: the full application starts against a real Postgres. */
class ApplicationIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;
    @Autowired
    MockMvc mvc;

    // fails if Flyway did not run (e.g. missing spring-boot-starter-flyway on Boot 4)
    @Test
    void flywayMigrationsApplied() {
        Integer users = jdbcClient.sql("SELECT count(*) FROM users").query(Integer.class).single();
        assertThat(users).isGreaterThanOrEqualTo(2); // seeded by V2__insert_sample_data.sql
    }

    // the endpoint the EC2 deploy and platform health checks rely on, reachable without login
    @Test
    void healthEndpointIsPublicAndUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
