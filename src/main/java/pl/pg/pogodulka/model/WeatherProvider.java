package pl.pg.pogodulka.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Źródło danych pogodowych. WeatherService zna tylko ten interfejs.
 * <p>
 * Implementacja to {@code @Component} z
 * {@code @ConditionalOnProperty(name = "weather.provider", havingValue = "<nazwa>")}.
 * Zwraca wyłącznie nasz model, nigdy JSON zewnętrznego API.
 * Timeout i błędy zewnętrznego źródła zamienia na {@link ProviderUnavailableException}.
 */
public interface WeatherProvider {

    /**
     * Zamienia nazwę miasta na współrzędne (pierwszy wynik geokodowania).
     *
     * @return {@code Optional.empty()}, gdy miasta nie ma
     */
    Optional<Location> findCity(String name);

    /**
     * Prognoza dzienna.
     *
     * @param location {@code location.name()} może być {@code null} (zapytanie po lat/lon)
     * @return po jednym dniu na każdą datę od {@code from} do {@code to} włącznie, rosnąco
     */
    List<DailyWeather> getDaily(Location location, LocalDate from, LocalDate to);
}
