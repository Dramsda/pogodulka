package pl.pg.pogodulka.provider;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pl.pg.pogodulka.model.DailyWeather;
import pl.pg.pogodulka.model.Location;
import pl.pg.pogodulka.model.ProviderUnavailableException;
import pl.pg.pogodulka.model.WeatherProvider;

/**
 * Dane udawane do testów i demo. Ta sama lokalizacja i data dają zawsze tę samą pogodę.
 * <p>
 * Zarezerwowane miasta: {@code Atlantyda} (brak miasta → 404), {@code Awaria} (awaria źródła → 503).
 * Każde inne miasto „istnieje”, z współrzędnymi wyliczonymi z nazwy.
 */
@Component
@ConditionalOnProperty(name = "weather.provider", havingValue = "fake")
public class FakeWeatherProvider implements WeatherProvider {

    static final String UNKNOWN_CITY = "Atlantyda";
    static final String FAILING_CITY = "Awaria";

    private static final Map<String, Location> KNOWN_CITIES = Map.of(
            "kraków", new Location("Kraków", 50.0614, 19.9366),
            "warszawa", new Location("Warszawa", 52.2297, 21.0122),
            "gdańsk", new Location("Gdańsk", 54.3520, 18.6466),
            "zakopane", new Location("Zakopane", 49.2992, 19.9496));

    /** Kody WMO, z których losujemy: słońce, chmury, mgła, deszcz, śnieg, burza. */
    private static final int[] WMO_CODES = {0, 1, 2, 3, 45, 61, 63, 80, 71, 95};

    @Override
    public Optional<Location> findCity(String name) {
        if (name.equalsIgnoreCase(UNKNOWN_CITY)) {
            return Optional.empty();
        }
        Location known = KNOWN_CITIES.get(name.toLowerCase());
        if (known != null) {
            return Optional.of(known);
        }
        Random random = new Random(name.toLowerCase().hashCode());
        return Optional.of(new Location(name, round(49 + random.nextDouble() * 5), round(14 + random.nextDouble() * 10)));
    }

    @Override
    public List<DailyWeather> getDaily(Location location, LocalDate from, LocalDate to) {
        if (FAILING_CITY.equalsIgnoreCase(location.name())) {
            throw new ProviderUnavailableException("Fake provider failure for city " + FAILING_CITY);
        }
        return from.datesUntil(to.plusDays(1))
                .map(date -> generate(location, date))
                .toList();
    }

    private static DailyWeather generate(Location location, LocalDate date) {
        Random random = new Random(31L * Double.hashCode(location.lat() * 31 + location.lon()) + date.toEpochDay());
        int code = WMO_CODES[random.nextInt(WMO_CODES.length)];
        double tempMin = round(-5 + random.nextDouble() * 15);
        double tempMax = round(tempMin + 2 + random.nextDouble() * 8);
        boolean wet = code >= 51;
        double precipitation = wet ? round(0.5 + random.nextDouble() * 15) : 0.0;
        double wind = round(3 + random.nextDouble() * (code >= 95 ? 60 : 30));
        return new DailyWeather(date, tempMin, tempMax, precipitation, wind, code);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
