package pl.pg.pogodulka.model;

/**
 * Źródło danych nie odpowiada albo zwraca błąd. Mapowane na 503 z nagłówkiem Retry-After.
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderUnavailableException(String message) {
        super(message);
    }
}
