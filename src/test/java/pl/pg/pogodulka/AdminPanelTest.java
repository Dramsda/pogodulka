package pl.pg.pogodulka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Panel admina: wewnętrzny, poza kontraktem. Lista requestów do /api/** za hasłem (HTTP Basic).
 */
@SpringBootTest(properties = {"weather.provider=fake", "weather.admin.password=secret"})
@AutoConfigureMockMvc
class AdminPanelTest {

    @TempDir
    static Path dir;

    @DynamicPropertySource
    static void requestLogFile(DynamicPropertyRegistry registry) {
        registry.add("weather.request-log.file", () -> dir.resolve("requests.jsonl").toString());
    }

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void requests_noPassword() {
        assertThat(mockMvc.get().uri("/admin/api/requests"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"weather-admin\"");
    }

    @Test
    void requests_wrongPassword() {
        assertThat(mockMvc.get().uri("/admin/api/requests").header(HttpHeaders.AUTHORIZATION, basic("admin", "nope")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void page_noPassword() {
        assertThat(mockMvc.get().uri("/admin/index.html"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void page() {
        assertThat(mockMvc.get().uri("/admin/index.html").header(HttpHeaders.AUTHORIZATION, adminAuth()))
                .hasStatusOk()
                .bodyText().contains("/admin/api/requests");
    }

    @Test
    void requests_showsSuccessfulCall() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Kraków&date={date}", tomorrow()))
                .hasStatusOk();

        // Lista jest od najnowszego, więc $[0] to request z tego testu.
        var newest = assertThat(mockMvc.get().uri("/admin/api/requests").header(HttpHeaders.AUTHORIZATION, adminAuth()))
                .hasStatusOk()
                .bodyJson();
        newest.extractingPath("$[0].method").isEqualTo("GET");
        newest.extractingPath("$[0].path").isEqualTo("/api/weather/day?city=Kraków&date=" + tomorrow());
        newest.extractingPath("$[0].status").isEqualTo(200);
        newest.extractingPath("$[0].clientIp").isEqualTo("127.0.0.1");
        newest.extractingPath("$[0].error").isNull();
        newest.extractingPath("$[0].location").isEqualTo("Kraków");
        newest.hasPath("$[0].time")
                .hasPath("$[0].durationMs")
                .hasPath("$[0].categories[0]")
                .doesNotHavePath("$[0].categories[1]");
    }

    @Test
    void requests_showsError() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Atlantyda&date={date}", tomorrow()))
                .hasStatus(HttpStatus.NOT_FOUND);

        var newest = assertThat(mockMvc.get().uri("/admin/api/requests").header(HttpHeaders.AUTHORIZATION, adminAuth()))
                .hasStatusOk()
                .bodyJson();
        newest.extractingPath("$[0].status").isEqualTo(404);
        newest.extractingPath("$[0].error").isEqualTo("City Atlantyda not found");
        newest.doesNotHavePath("$[0].categories[0]");
    }

    @Test
    void requests_savedToFile() throws Exception {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Gdańsk&date={date}", tomorrow()))
                .hasStatusOk();

        assertThat(Files.readString(dir.resolve("requests.jsonl"))).contains("city=Gdańsk");
    }

    private static String adminAuth() {
        return basic("admin", "secret");
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }
}
