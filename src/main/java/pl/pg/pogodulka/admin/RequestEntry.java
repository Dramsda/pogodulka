package pl.pg.pogodulka.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Jeden request do /api/** widoczny w panelu admina.
 *
 * @param location   nazwa miasta albo "lat, lon"; null, gdy odpowiedź nie miała lokalizacji (błąd)
 * @param categories kategorie pogody z odpowiedzi, po jednej na dzień; pusta przy błędzie
 * @param days       skrót odpowiedzi dla panelu (data, kategoria, temperatury); null we wpisach sprzed tej zmiany
 */
public record RequestEntry(
        Instant time,
        String method,
        String path,
        int status,
        long durationMs,
        String clientIp,
        String error,
        String location,
        List<String> categories,
        List<DaySummary> days) {

    public record DaySummary(LocalDate date, String category, double tempMin, double tempMax) {
    }
}
