package pl.pg.pogodulka.model;

import java.time.LocalDate;

/**
 * Pogoda jednego dnia, w jednostkach Open-Meteo.
 *
 * @param tempMin         °C
 * @param tempMax         °C
 * @param precipitationMm suma opadów w ciągu dnia, mm
 * @param windKph         maksymalna prędkość wiatru, km/h
 * @param weatherCode     kod WMO; kategorię (RAIN itd.) wylicza WeatherService
 */
public record DailyWeather(
        LocalDate date,
        double tempMin,
        double tempMax,
        double precipitationMm,
        double windKph,
        int weatherCode) {
}
