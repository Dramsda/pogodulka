package pl.pg.pogodulka.model;

/**
 * Błędne parametry żądania. Mapowane na 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
