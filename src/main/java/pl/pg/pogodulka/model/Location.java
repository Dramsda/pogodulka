package pl.pg.pogodulka.model;

/**
 * @param name nazwa miasta albo {@code null}, gdy pytano po współrzędnych
 */
public record Location(String name, double lat, double lon) {
}
