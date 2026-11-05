package com.abitmanipulator.url_shortner;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base class for full-context integration tests (*IT, run by Failsafe in `./mvnw verify`).
 * <p>
 * Every subclass shares the exact same configuration, so Spring's test-context cache boots the
 * application (and one Postgres container) once for all of them instead of once per class.
 * The URL HEAD check is switched off so tests never depend on the internet.
 */
@SpringBootTest(properties = "app.validate-original-url=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
