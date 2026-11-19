package pl.pg.pogodulka.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.pg.pogodulka.dto.DayResponse;
import pl.pg.pogodulka.dto.WeatherCategory;
import pl.pg.pogodulka.dto.WeatherResponse;
import pl.pg.pogodulka.model.BadRequestException;
import pl.pg.pogodulka.model.DailyWeather;
import pl.pg.pogodulka.model.Location;
import pl.pg.pogodulka.model.NotFoundException;
import pl.pg.pogodulka.model.WeatherProvider;

@Service
@RequiredArgsConstructor
public class WeatherService {

    /** Prognoza obejmuje dziś i kolejne 15 dni. */
    static final int MAX_DAYS_AHEAD = 15;

    private final WeatherProvider provider;

    public WeatherResponse getWeather(String city, Double lat, Double lon, LocalDate from, LocalDate to) {
        // Najpierw cała walidacja, żeby przy 400 nie wołać zewnętrznego API.
        validateDates(from, to, LocalDate.now());
        Location location = resolveLocation(city, lat, lon);

        List<DayResponse> days = provider.getDaily(location, from, to).stream()
                .map(WeatherService::toDay)
                .toList();
        return new WeatherResponse(location, days, false);
    }

    static void validateDates(LocalDate from, LocalDate to, LocalDate today) {
        if (from.isAfter(to)) {
            throw new BadRequestException("Date 'from' (%s) is after 'to' (%s)".formatted(from, to));
        }
        LocalDate last = today.plusDays(MAX_DAYS_AHEAD);
        if (from.isBefore(today) || to.isAfter(last)) {
            throw new BadRequestException("Dates must be between %s and %s".formatted(today, last));
        }
    }

    private Location resolveLocation(String city, Double lat, Double lon) {
        boolean hasCity = city != null && !city.isBlank();
        boolean hasCoords = lat != null || lon != null;

        if (hasCity && hasCoords) {
            throw new BadRequestException("Provide either 'city' or 'lat'/'lon', not both");
        }
        if (!hasCity) {
            return coordinates(lat, lon);
        }
        String name = city.strip();
        return provider.findCity(name)
                .orElseThrow(() -> new NotFoundException("City %s not found".formatted(name)));
    }

    private static Location coordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new BadRequestException("Provide 'city' or both 'lat' and 'lon'");
        }
        // Zapis przez negację, bo NaN nie spełnia żadnego porównania i inaczej przechodzi walidację.
        if (!(lat >= -90 && lat <= 90)) {
            throw new BadRequestException("'lat' must be between -90 and 90");
        }
        if (!(lon >= -180 && lon <= 180)) {
            throw new BadRequestException("'lon' must be between -180 and 180");
        }
        return new Location(null, lat, lon);
    }

    private static DayResponse toDay(DailyWeather d) {
        return new DayResponse(d.date(), d.tempMin(), d.tempMax(), d.precipitationMm(), d.windKph(),
                category(d.weatherCode()));
    }

    /**
     * Kod WMO (Open-Meteo {@code weather_code}) → kategoria. Nieznany kod traktujemy jak zachmurzenie.
     */
    static WeatherCategory category(int wmoCode) {
        return switch (wmoCode) {
            case 0, 1 -> WeatherCategory.SUNNY;
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCategory.RAIN;
            case 71, 73, 75, 77, 85, 86 -> WeatherCategory.SNOW;
            case 95, 96, 99 -> WeatherCategory.STORM;
            default -> WeatherCategory.CLOUDY; // 2, 3, 45, 48 i nieznane
        };
    }
}
