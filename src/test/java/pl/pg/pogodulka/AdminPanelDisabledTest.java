package pl.pg.pogodulka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Bez ustawionego hasła panel jest wyłączony: nawet pusty login/hasło nie przechodzi.
 */
@SpringBootTest(properties = "weather.provider=fake")
@AutoConfigureMockMvc
class AdminPanelDisabledTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void requests_noPasswordConfigured() {
        String emptyCredentials = "Basic " + Base64.getEncoder().encodeToString("admin:".getBytes(StandardCharsets.UTF_8));

        assertThat(mockMvc.get().uri("/admin/api/requests").header(HttpHeaders.AUTHORIZATION, emptyCredentials))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().hasPath("$.error");
    }
}
