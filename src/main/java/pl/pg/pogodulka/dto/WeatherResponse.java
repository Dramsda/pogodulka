package pl.pg.pogodulka.dto;

import java.util.List;

import pl.pg.pogodulka.model.Location;

/**
 * Odpowiedź /day i /range. {@code stale} jest obecnie zawsze {@code false}.
 */
public record WeatherResponse(Location location, List<DayResponse> days, boolean stale) {
}
