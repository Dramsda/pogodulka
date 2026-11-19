package pl.pg.pogodulka.dto;

import java.time.LocalDate;

public record DayResponse(
        LocalDate date,
        double tempMin,
        double tempMax,
        double precipitationMm,
        double windKph,
        WeatherCategory category) {
}
