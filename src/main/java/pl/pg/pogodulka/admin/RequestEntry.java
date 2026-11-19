package pl.pg.pogodulka.admin;

import java.time.Instant;
import java.util.List;

/**
 * Jeden request do /api/** widoczny w panelu admina.
 *
 * @param location   nazwa miasta albo "lat, lon"; null, gdy odpowiedź nie miała lokalizacji (błąd)
 * @param categories kategorie pogody z odpowiedzi, po jednej na dzień; pusta przy błędzie
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
        List<String> categories) {
}
