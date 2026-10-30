package com.abitmanipulator.url_shortner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringBootUrlShortnerApplicationTests {

	@Autowired
	JdbcClient jdbcClient;

	@Test
	void contextLoads() {
	}

	// fails if Flyway did not run (e.g. missing spring-boot-starter-flyway on Boot 4)
	@Test
	void flywayMigrationsApplied() {
		Integer users = jdbcClient.sql("SELECT count(*) FROM users").query(Integer.class).single();
		assertThat(users).isGreaterThanOrEqualTo(2); // seeded by V2__insert_sample_data.sql
	}

}
