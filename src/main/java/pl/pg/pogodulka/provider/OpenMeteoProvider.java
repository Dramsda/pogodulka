package pl.pg.pogodulka.provider;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.pg.pogodulka.model.DailyWeather;
import pl.pg.pogodulka.model.Location;
import pl.pg.pogodulka.model.ProviderUnavailableException;
import pl.pg.pogodulka.model.WeatherProvider;

/**
 * Prognoza i geokodowanie z Open-Meteo (bez klucza API).
 * Timeout i każdy błąd HTTP zamienia na {@link ProviderUnavailableException}.
 */
@Component
@ConditionalOnProperty(name = "weather.provider", havingValue = "open-meteo")
public class OpenMeteoProvider implements WeatherProvider {

    private static final String DAILY_FIELDS =
            "weather_code,temperature_2m_min,temperature_2m_max,precipitation_sum,wind_speed_10m_max";

    private final RestClient geocoding;
    private final RestClient forecast;

    public OpenMeteoProvider(
            RestClient.Builder builder,
            @Value("${weather.open-meteo.geocoding-url:https://geocoding-api.open-meteo.com}") String geocodingUrl,
            @Value("${weather.open-meteo.forecast-url:https://api.open-meteo.com}") String forecastUrl,
            @Value("${weather.open-meteo.timeout:3s}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        RestClient.Builder configured = builder.requestFactory(requestFactory);
        this.geocoding = configured.clone().baseUrl(geocodingUrl).build();
        this.forecast = configured.clone().baseUrl(forecastUrl).build();
    }

    @Override
    public Optional<Location> findCity(String name) {
        GeocodingResponse response = call("geocoding", () -> geocoding.get()
                .uri(uri -> uri.path("/v1/search")
                        .queryParam("name", name)
                        .queryParam("count", 1)
                        .queryParam("language", "pl")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(GeocodingResponse.class));

        if (response == null || response.results() == null || response.results().isEmpty()) {
            return Optional.empty();
        }
        GeocodingResult first = response.results().getFirst();
        return Optional.of(new Location(first.name(), first.latitude(), first.longitude()));
    }

    @Override
    public List<DailyWeather> getDaily(Location location, LocalDate from, LocalDate to) {
        ForecastResponse response = call("forecast", () -> forecast.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", location.lat())
                        .queryParam("longitude", location.lon())
                        .queryParam("daily", DAILY_FIELDS)
                        .queryParam("timezone", "auto")
                        .queryParam("start_date", from)
                        .queryParam("end_date", to)
                        .build())
                .retrieve()
                .body(ForecastResponse.class));

        if (response == null || response.daily() == null || response.daily().time() == null) {
            throw new ProviderUnavailableException("Open-Meteo returned no daily forecast");
        }
        return toDailyWeather(response.daily());
    }

    private static List<DailyWeather> toDailyWeather(Daily daily) {
        List<DailyWeather> days = new ArrayList<>();
        for (int i = 0; i < daily.time().size(); i++) {
            Integer code = at(daily.weatherCode(), i);
            Double tempMin = at(daily.tempMin(), i);
            Double tempMax = at(daily.tempMax(), i);
            Double precipitation = at(daily.precipitation(), i);
            Double wind = at(daily.windMax(), i);
            if (code == null || tempMin == null || tempMax == null || precipitation == null || wind == null) {
                throw new ProviderUnavailableException("Open-Meteo returned incomplete data for " + daily.time().get(i));
            }
            days.add(new DailyWeather(daily.time().get(i), tempMin, tempMax, precipitation, wind, code));
        }
        return days;
    }

    private static <T> T at(List<T> values, int i) {
        return values != null && i < values.size() ? values.get(i) : null;
    }

    private static <T> T call(String what, java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("Open-Meteo " + what + " request failed", e);
        }
    }

    // --- JSON Open-Meteo; nie wychodzi poza tę klasę ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResponse(List<GeocodingResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodingResult(String name, double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastResponse(Daily daily) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Daily(
            List<LocalDate> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_min") List<Double> tempMin,
            @JsonProperty("temperature_2m_max") List<Double> tempMax,
            @JsonProperty("precipitation_sum") List<Double> precipitation,
            @JsonProperty("wind_speed_10m_max") List<Double> windMax) {
    }
}
