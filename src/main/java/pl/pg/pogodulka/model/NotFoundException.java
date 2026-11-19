package pl.pg.pogodulka.model;

/**
 * Nie znaleziono zasobu (np. miasta). Mapowane na 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
