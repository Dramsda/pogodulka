package pl.pg.pogodulka;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Kontrakt z docs/kontrakt-pogoda.md sprawdzany przez HTTP. Dane z FakeWeatherProvider.
 */
@SpringBootTest(properties = "weather.provider=fake")
@AutoConfigureMockMvc
class ContractTest {

    @Autowired
    private MockMvcTester mockMvc;

    // --- 200 ---

    @Test
    void getDay() {
        assertThat(mockMvc.get().uri("/api/weather/day?city={city}&date={date}", "Kraków", tomorrow()))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "location": { "name": "Kraków" },
                          "days": [ { "date": "%s" } ],
                          "stale": false
                        }
                        """.formatted(tomorrow()))
                .hasPath("$.location.lat")
                .hasPath("$.location.lon")
                .hasPath("$.days[0].tempMin")
                .hasPath("$.days[0].tempMax")
                .hasPath("$.days[0].precipitationMm")
                .hasPath("$.days[0].windKph")
                .hasPath("$.days[0].category")
                .doesNotHavePath("$.days[1]");
    }

    @Test
    void getDay_byCoordinates() {
        assertThat(mockMvc.get().uri("/api/weather/day?lat=50.0614&lon=19.9366&date={date}", tomorrow()))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "location": { "name": null, "lat": 50.0614, "lon": 19.9366 },
                          "days": [ { "date": "%s" } ],
                          "stale": false
                        }
                        """.formatted(tomorrow()));
    }

    @Test
    void getDay_today() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Kraków&date={date}", today()))
                .hasStatusOk();
    }

    @Test
    void getDay_lastForecastDay() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Kraków&date={date}", today().plusDays(15)))
                .hasStatusOk();
    }

    @Test
    void getRange() {
        LocalDate from = tomorrow();
        assertThat(mockMvc.get().uri("/api/weather/range?city=Kraków&from={from}&to={to}", from, from.plusDays(2)))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "location": { "name": "Kraków" },
                          "days": [ { "date": "%s" }, { "date": "%s" }, { "date": "%s" } ],
                          "stale": false
                        }
                        """.formatted(from, from.plusDays(1), from.plusDays(2)))
                .doesNotHavePath("$.days[3]");
    }

    @Test
    void getRange_byCoordinates() {
        LocalDate from = tomorrow();
        assertThat(mockMvc.get().uri("/api/weather/range?lat=50.0614&lon=19.9366&from={from}&to={to}", from, from.plusDays(1)))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        { "location": { "name": null, "lat": 50.0614, "lon": 19.9366 }, "stale": false }
                        """)
                .hasPath("$.days[1]")
                .doesNotHavePath("$.days[2]");
    }

    @Test
    void getRange_maxLength() {
        assertThat(mockMvc.get().uri("/api/weather/range?city=Kraków&from={from}&to={to}", today(), today().plusDays(15)))
                .hasStatusOk()
                .bodyJson()
                .hasPath("$.days[15]")
                .doesNotHavePath("$.days[16]");
    }

    // --- 400 ---

    @Test
    void getDay_missingLocation() {
        assertBadRequest("/api/weather/day?date=" + tomorrow());
    }

    @Test
    void getDay_cityAndCoordinates() {
        assertBadRequest("/api/weather/day?city=Kraków&lat=50.0614&lon=19.9366&date=" + tomorrow());
    }

    @Test
    void getDay_latWithoutLon() {
        assertBadRequest("/api/weather/day?lat=50.0614&date=" + tomorrow());
    }

    @Test
    void getDay_latOutOfRange() {
        assertBadRequest("/api/weather/day?lat=91&lon=19.9366&date=" + tomorrow());
    }

    @Test
    void getDay_lonOutOfRange() {
        assertBadRequest("/api/weather/day?lat=50.0614&lon=-181&date=" + tomorrow());
    }

    @Test
    void getDay_latNotANumber() {
        assertBadRequest("/api/weather/day?lat=abc&lon=19.9366&date=" + tomorrow());
    }

    @Test
    void getDay_missingDate() {
        assertBadRequest("/api/weather/day?city=Kraków");
    }

    @Test
    void getDay_invalidDateFormat() {
        assertBadRequest("/api/weather/day?city=Kraków&date=20.11.2026");
    }

    @Test
    void getDay_pastDate() {
        assertBadRequest("/api/weather/day?city=Kraków&date=" + today().minusDays(1));
    }

    @Test
    void getDay_dateTooFar() {
        assertBadRequest("/api/weather/day?city=Kraków&date=" + today().plusDays(16));
    }

    @Test
    void getRange_fromAfterTo() {
        assertBadRequest("/api/weather/range?city=Kraków&from=" + tomorrow().plusDays(2) + "&to=" + tomorrow());
    }

    @Test
    void getRange_pastFrom() {
        assertBadRequest("/api/weather/range?city=Kraków&from=" + today().minusDays(1) + "&to=" + tomorrow());
    }

    @Test
    void getRange_toTooFar() {
        assertBadRequest("/api/weather/range?city=Kraków&from=" + tomorrow() + "&to=" + today().plusDays(16));
    }

    @Test
    void getRange_missingTo() {
        assertBadRequest("/api/weather/range?city=Kraków&from=" + tomorrow());
    }

    // --- 404 ---

    @Test
    void getDay_unknownCity() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Atlantyda&date={date}", tomorrow()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "error": "City Atlantyda not found" }
                        """);
    }

    @Test
    void getRange_unknownCity() {
        assertThat(mockMvc.get().uri("/api/weather/range?city=Atlantyda&from={from}&to={to}", tomorrow(), tomorrow()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "error": "City Atlantyda not found" }
                        """);
    }

    // --- 503 ---

    @Test
    void getDay_providerUnavailable() {
        assertThat(mockMvc.get().uri("/api/weather/day?city=Awaria&date={date}", tomorrow()))
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .hasHeader("Retry-After", "60")
                .bodyJson().hasPath("$.error");
    }

    @Test
    void getRange_providerUnavailable() {
        assertThat(mockMvc.get().uri("/api/weather/range?city=Awaria&from={from}&to={to}", tomorrow(), tomorrow()))
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .hasHeader("Retry-After", "60")
                .bodyJson().hasPath("$.error");
    }

    // --- helpers ---

    private void assertBadRequest(String uri) {
        assertThat(mockMvc.get().uri(uri))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().hasPath("$.error");
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    private LocalDate tomorrow() {
        return today().plusDays(1);
    }
}
